# Keystone 限流设计

## 1. 背景

Keystone 的限流能力通过 `@RateLimit` 注解应用在 Controller 方法上。早期设计允许每个注解自行选择 Redis 或本地 Map 作为限流存储，这会把业务规则和运行时基础设施选择混在一起：

- 接口作者需要理解部署形态，才能决定 `cacheType`。
- 多节点生产环境中误用本地内存会削弱全局限流。
- Redis 和本地实现必须长期保持完全一致，否则同一个注解会因为存储类型不同而表现不同。
- Redis 故障时是否降级属于系统运行策略，不应由单个接口注解决定。

新的设计将注解收敛为纯业务规则，后端实现由全局配置统一控制。

## 2. 设计目标

1. `@RateLimit` 只描述限流规则，不暴露 Redis/本地内存等实现选择。
2. 生产默认使用 Redis，保证多实例部署时限流计数共享。
3. Redis 不可用时默认失败，不静默降级，避免集群限流被悄悄放大。
4. 是否降级到本地内存必须通过全局配置显式开启。
5. Redis 和本地内存实现保持相同的固定窗口语义。
6. 限流 key 生成集中管理，避免不同实现生成不同 key。

## 3. 非目标

| 非目标 | 说明 |
| --- | --- |
| 滑动窗口限流 | 当前实现是固定窗口计数，满足登录、验证码、公钥等接口保护需求 |
| 分布式降级一致性 | 本地降级只保证单 JVM 内有效，不提供集群一致性 |
| 每个接口选择后端 | 后端选择属于部署策略，不属于接口规则 |
| 自动探测 Redis 健康并静默降级 | Redis 故障默认应暴露为错误，防止生产限流弱化 |

## 4. 使用方式

接口只声明规则：

```java
@RateLimit(key = RateLimitKey.LOGIN_CAPTCHA_KEY, time = 10, maxCount = 10, limitType = LimitType.IP)
@GetMapping("/captchaImage")
public ResponseDTO<CaptchaDTO> getCaptchaImg() {
    ...
}
```

配置后端策略：

```yaml
keystone:
  rate-limit:
    backend: redis
    fallback-to-local: false
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `keystone.rate-limit.backend` | `redis` | 限流存储后端，支持 `redis`、`local` |
| `keystone.rate-limit.fallback-to-local` | `false` | Redis 限流缓存失败时是否降级到本地内存 |

环境变量：

```text
KEYSTONE_RATE_LIMIT_BACKEND=redis
KEYSTONE_RATE_LIMIT_FALLBACK_TO_LOCAL=false
```

## 5. 模块结构

```text
app.keystone.infrastructure.annotations.ratelimit
  RateLimit                 限流规则注解
  RateLimitBackend          全局后端枚举
  RateLimitChecker          限流统一入口
  RateLimitKey              业务 key 常量
  RateLimitKeyGenerator     限流 key 生成器
  RateLimitProperties       keystone.rate-limit 配置
  RateLimiterAspect         AOP 切面

app.keystone.infrastructure.annotations.ratelimit.implementation
  AbstractRateLimitChecker  参数校验
  RedisRateLimitChecker     Redis 固定窗口计数
  LocalRateLimitChecker     本地固定窗口计数
```

## 6. 执行链路

```text
Controller method
  -> RateLimiterAspect
  -> RateLimitChecker
      -> backend=local
          -> LocalRateLimitChecker
      -> backend=redis
          -> RedisRateLimitChecker
          -> Redis GET_CACHE_FAILED 且 fallback-to-local=true
              -> LocalRateLimitChecker
```

`RateLimiterAspect` 不再知道 Redis 或本地内存的存在，只依赖统一入口 `RateLimitChecker`。

## 7. 限流 key 设计

所有实现都通过 `RateLimitKeyGenerator` 生成 key。

格式：

```text
<base-key>:<limit-type>:<discriminator>
```

示例：

```text
Rate-Limit\:Login-Captcha:IP:10.0.0.1
Rate-Limit\:Test:GLOBAL:GLOBAL
Rate-Limit\:User:APP_USER:USER\:42
```

规则：

1. `base-key` 为空时使用 `RateLimitKey.PREFIX`。
2. `base-key` 末尾的 `:` 会被规范化移除。
3. 每个片段都会转义 `:` 和 `\`，避免分隔符歧义。
4. `GLOBAL` 使用固定判别值 `GLOBAL`。
5. `IP` 从当前 Servlet request 中解析客户端 IP。
6. `SYSTEM_USER` 和 `APP_USER` 要求当前 `Authentication` 已认证。
7. 用户维度优先使用 `userId`，其次 `username`，最后 `cachedKey`。

## 8. 后端策略

### 8.1 Redis 后端

Redis 是默认后端，适合生产和多实例部署。

行为：

1. 使用 Lua 脚本保证计数和过期设置的原子性。
2. 第一次访问时 `INCR` 并设置 `EXPIRE`。
3. 窗口内当前计数超过 `maxCount` 时拒绝。
4. Redis 调用失败或返回空值时抛出 `GET_CACHE_FAILED`。

### 8.2 本地后端

本地后端使用 JVM 内 Guava Cache 保存固定窗口计数，只适合：

- 本地开发。
- 单实例部署。
- Redis 故障时显式允许的临时降级。

限制：

- 多节点之间不共享计数。
- 节点数越多，整体允许请求数可能按节点数放大。
- 进程重启后计数丢失。

### 8.3 降级策略

默认：

```yaml
fallback-to-local: false
```

原因是生产环境中，Redis 故障后自动降级本地内存会把全局限流变成每节点限流。例如 4 个节点、`10 秒 10 次`，实际可能变成 `10 秒 40 次`。这类静默弱化比直接失败更难发现。

只有在明确接受该风险时才开启：

```yaml
keystone:
  rate-limit:
    backend: redis
    fallback-to-local: true
```

开启后，仅 Redis 缓存失败会降级。业务侧的超限异常不会降级。

## 9. 固定窗口语义

Redis 和本地内存都使用固定窗口：

```text
time = 10
maxCount = 5
```

表示同一个 key 在 10 秒窗口内最多允许 5 次。第 6 次抛出 `COMMON_REQUEST_TOO_OFTEN`。窗口过期后重新计数。

`time <= 0` 或 `maxCount <= 0` 是配置错误，会抛出 `INVALID_PARAMETER`。

## 10. 推荐配置

### 10.1 生产多实例

```yaml
keystone:
  rate-limit:
    backend: redis
    fallback-to-local: false
```

### 10.2 本地开发

```yaml
keystone:
  rate-limit:
    backend: local
```

### 10.3 单实例临时容错

```yaml
keystone:
  rate-limit:
    backend: redis
    fallback-to-local: true
```

仅建议用于低风险、单实例或明确可接受限流弱化的环境。

## 11. 测试要求

限流相关改动至少运行：

```powershell
.\gradlew.bat :keystone-infrastructure:test --tests "app.keystone.infrastructure.annotations.*" --tests "app.keystone.infrastructure.annotations.ratelimit.*" --tests "app.keystone.infrastructure.annotations.ratelimit.implementation.*"
```

基础设施模块完整验证：

```powershell
.\gradlew.bat :keystone-infrastructure:test
.\gradlew.bat :keystone-infrastructure:check
```

当前测试覆盖：

- key 生成、转义、IP 上下文、用户认证边界。
- Redis 固定窗口脚本调用、超限、缓存失败、参数校验。
- 本地固定窗口计数、窗口过期、参数校验。
- 统一 `RateLimitChecker` 的 Redis 默认、本地后端、默认不降级、显式降级、客户端超限不降级。
