# 同一账号只允许一个在线登录设计

## 背景

Keystone 使用无状态 JWT 承载 tokenId，并把真实登录用户信息缓存在 Redis `login_tokens:` 中。历史实现只按 tokenId 保存会话，因此同一账号可以多次登录，每次登录都会生成新的 tokenId 和 Redis 会话。

当前业务要求改为：同一账号同一时间只能有一个 Keystone 在线会话。当该账号已有有效会话时，新的登录请求应被拒绝，并提示“该账号已经登录”。

## 目标

1. 同一 `sys_user.user_id` 只允许一个有效 Keystone 登录会话。
2. 第二次登录不踢掉旧会话，而是拒绝新登录。
3. 正常退出、监控页强退、token 自动续期都要同步维护账号占用状态。
4. Redis 中残留的过期账号占用标记不应永久阻塞登录。
5. 本地登录、mixed/Keylo 凭证登录、`/login/keylo` 兼容入口都走同一套限制。

## 非目标

1. 不限制直接携带 Keylo accessToken 访问受保护接口的临时主体。该路径不是 Keystone 登录会话，不写入 `login_tokens:`。
2. 不实现“新登录踢掉旧登录”。当前语义是“已有在线会话时拒绝新登录”。
3. 不做多端类型区分，例如 PC、移动端分别允许一个会话。

## Redis Key 设计

Keystone 维护两类登录缓存：

| Key 前缀 | 值 | 用途 |
| --- | --- | --- |
| `login_tokens:{tokenId}` | `SystemLoginUser` | 保存当前 tokenId 对应的登录用户、权限、登录 IP、浏览器、登录时间等会话信息 |
| `login_accounts:{accountId}` | `tokenId` | 保存账号当前占用的 tokenId，用于判断同一账号是否已经在线 |

`accountId` 优先使用 `SystemLoginUser.userId`。如果未来出现无 `userId` 的 Keystone 登录主体，则兜底使用 `username`。

两个 key 使用相同过期时间，当前由 `CacheKeyEnum.LOGIN_USER_KEY` 和 `CacheKeyEnum.LOGIN_ACCOUNT_KEY` 定义为 30 分钟。token 自动续期时会同时刷新两类 key。

## 登录流程

`LoginService` 在本地认证或 Keylo 认证成功后，会调用 `TokenService#createTokenAndPutUserInCache` 创建 Keystone token。

核心流程：

1. 生成新的 `tokenId`。
2. 写入 `login_tokens:{tokenId}`。
3. 检查 `login_accounts:{accountId}`：
   - 如果不存在，使用 Redis `SET NX EX` 语义原子写入账号占用标记。
   - 如果存在，读取其中的旧 `tokenId`，再检查 `login_tokens:{oldTokenId}` 是否存在。
   - 如果旧 token 仍有效，抛出 `Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN`。
   - 如果旧 token 已过期或不存在，删除残留账号标记，然后重新尝试原子占用。
4. 账号占用成功后签发 JWT，JWT claim 中仍只保存 `login_user_key={tokenId}`。
5. `LoginService` 再记录登录成功日志和更新用户最近登录信息。

如果账号占用失败，`TokenService` 会删除本次临时写入的 `login_tokens:{tokenId}`，避免产生无主在线会话。

## 并发控制

账号占用使用 `RedisCacheTemplate#setIfAbsent`，底层调用 Redis `SET key value NX EX ttl`，避免两个并发登录请求同时通过“先查后写”的窗口。

并发场景下：

1. 两个请求可能都先写入各自的 `login_tokens:{tokenId}`。
2. 只有一个请求能成功写入 `login_accounts:{accountId}`。
3. 失败的一方会删除自己的 `login_tokens:{tokenId}`，并返回“该账号已经登录”。

因此最终 Redis 中只会留下一个账号占用标记和一个有效 Keystone 会话。

## 续期与释放

### token 自动续期

`JwtAuthenticationTokenFilter` 识别 Keystone token 后会调用 `TokenService#refreshToken`。当达到自动续期阈值时：

1. 刷新 `login_tokens:{tokenId}`。
2. 同步刷新 `login_accounts:{accountId}`，值保持为当前 `tokenId`。

这保证活跃会话不会因为账号占用 key 先过期而允许第二次登录。

### 正常退出

`/logout` 成功处理器调用 `TokenService#removeLoginUser(SystemLoginUser)`：

1. 删除 `login_tokens:{tokenId}`。
2. 读取 `login_accounts:{accountId}`。
3. 仅当账号占用值等于当前 `tokenId` 时删除账号占用标记。

最后一步避免误删其它会话的账号占用标记。

### 监控页强退

`/monitor/onlineUser/{tokenId}` 不再直接删除 `login_tokens:`，而是调用 `TokenService#removeLoginUser(String tokenId)`：

1. 先通过 tokenId 读取 `SystemLoginUser`。
2. 删除 `login_tokens:{tokenId}`。
3. 如果能读到登录用户，则按正常退出规则释放账号占用标记。

## 错误码

新增业务错误码：

| 错误码 | i18n key | 默认提示 |
| --- | --- | --- |
| `Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN` | `Business.LOGIN_ACCOUNT_ALREADY_LOGGED_IN` | `该账号已经登录` |

该错误会在登录接口返回给前端，前端可直接展示提示。

## 关键类

| 类 | 职责 |
| --- | --- |
| `TokenService` | Keystone token 创建、解析、续期、账号占用、登录态释放 |
| `LoginService` | 本地/Keylo 登录入口，认证成功后创建 Keystone token 并记录登录信息 |
| `SecurityConfig` | `/logout` 成功处理，委托 `TokenService` 释放登录态 |
| `MonitorController` | 在线用户强退，委托 `TokenService` 释放登录态 |
| `RedisCacheService` | 暴露 `loginUserCache` 和 `loginAccountCache` |
| `RedisCacheTemplate` | 封装 Redis 缓存读写和原子 `setIfAbsent` |

## 测试覆盖

`TokenServiceTest` 覆盖以下场景：

1. 创建 token 时仍包含标准 JWT claims。
2. 已有有效账号会话时拒绝第二次登录。
3. 账号占用标记残留但旧 token 已过期时，清理残留并允许登录。
4. 删除登录用户时同步删除匹配的账号占用标记。

登录链路回归覆盖：

1. `LoginServiceKeyloLoginTest`
2. `LoginServiceRsaPublicKeyTest`
3. `SecurityConfigSwaggerAccessTest`

建议提交前执行：

```bash
./gradlew.bat :keystone-admin:test --tests app.keystone.admin.customize.service.login.TokenServiceTest --tests app.keystone.admin.customize.service.login.LoginServiceKeyloLoginTest --tests app.keystone.admin.customize.service.login.LoginServiceRsaPublicKeyTest --tests app.keystone.admin.customize.config.SecurityConfigSwaggerAccessTest
```

## 运维说明

该功能不需要数据库迁移。上线后 Redis 中会新增 `login_accounts:` 前缀 key。

如果部署前 Redis 中已有旧版 `login_tokens:` 会话，这些旧会话没有对应 `login_accounts:` 标记。首次新登录会创建账号占用标记；旧 token 仍可能在自身过期前继续访问。需要严格切换时，可在发布时清理旧 `login_tokens:*`，要求用户重新登录。
