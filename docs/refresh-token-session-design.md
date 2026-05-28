# Keystone 自动刷新 token 设计

## 背景

Keystone 当前使用短期 JWT 作为访问令牌，JWT 中保存 `login_user_key={tokenId}`，真实登录用户信息缓存在 Redis `login_tokens:{tokenId}` 中。为了保证单账号登录状态一致，Redis 登录态 TTL 已调整为和 `token.expirationSeconds` 一致。

这能解决 Web 端 token 过期后账号占用残留的问题，但对桌面客户端不够友好。桌面客户端通常需要长时间运行，不能因为 access token 到期就频繁要求用户重新输入账号密码。因此需要引入完整的 refresh token 机制，而不是恢复“只刷新 Redis、不刷新 JWT”的旧逻辑。

## 目标

1. Web 端和桌面客户端都可以在 access token 过期前或收到认证失败后刷新 token。
2. 刷新成功必须返回新的 access token，并同步维护 Redis 会话和单账号登录占用。
3. 同一账号仍只允许一个 Keystone 在线会话。
4. refresh token 可被服务端撤销，退出登录和监控强退必须同时失效 access token 与 refresh token。
5. 刷新流程要能处理并发请求，避免多个请求同时刷新导致 token 状态错乱。

## 非目标

1. 不恢复只延长 Redis TTL 的“伪续期”方案。
2. 不支持同一账号多端同时在线。桌面客户端和 Web 客户端仍共享同一个账号占用。
3. 不把 Keylo accessToken 直接访问 Keystone API 的临时主体纳入 Keystone refresh token 会话；只有 Keystone 登录流程签发的会话参与刷新。

## Token 类型

| 类型 | 用途 | 建议有效期 | 存储位置 |
| --- | --- | --- | --- |
| access token | 调用 Keystone API | 30 分钟 | Web sessionStorage/Cookie；桌面客户端内存或安全存储 |
| refresh token | 换取新的 access token | 7 天，可配置 | Web sessionStorage/Cookie；桌面客户端系统凭据库或加密存储 |

access token 仍是 JWT。refresh token 建议使用高熵随机字符串，不使用 JWT，服务端只保存其哈希值，避免 Redis 泄露时可直接使用明文 refresh token。

## Redis Key 设计

新增和调整以下 key：

| Key 前缀 | 值 | TTL | 用途 |
| --- | --- | --- | --- |
| `login_tokens:{tokenId}` | `SystemLoginUser` | access token TTL | 当前 access token 对应的用户会话 |
| `login_refresh_tokens:{refreshTokenId}` | refresh token 会话对象 | refresh token TTL | 保存 refresh token 哈希、账号、当前 tokenId、状态 |
| `login_accounts:{accountId}` | `refreshTokenId` | refresh token TTL | 单账号在线占用，绑定长期会话而不是短期 access token |

refresh token 会话对象建议字段：

| 字段 | 说明 |
| --- | --- |
| `refreshTokenId` | 服务端生成的刷新会话 ID |
| `refreshTokenHash` | refresh token 哈希值 |
| `accountId` | `userId` 优先，缺失时使用 `username` |
| `currentTokenId` | 当前有效 access token 的 tokenId |
| `username` | 用户名，用于日志和兜底释放 |
| `issuedAt` | 签发时间 |
| `expiresAt` | refresh token 过期时间 |
| `revoked` | 是否已撤销 |

## 登录流程

1. 本地认证或 Keylo 凭证认证成功后，构建 `SystemLoginUser`。
2. 检查 `login_accounts:{accountId}`：
   - 不存在：允许创建新 refresh token 会话。
   - 存在：读取 `login_refresh_tokens:{refreshTokenId}`。
   - refresh 会话仍有效：拒绝登录，返回“该账号已经登录”。
   - refresh 会话不存在、过期或已撤销：删除残留账号占用后允许登录。
3. 生成 `refreshTokenId` 和 refresh token 明文。
4. 生成 access token 的 `tokenId` 和 JWT。
5. 写入：
   - `login_tokens:{tokenId}`，TTL 为 access token 有效期。
   - `login_refresh_tokens:{refreshTokenId}`，TTL 为 refresh token 有效期。
   - `login_accounts:{accountId} = refreshTokenId`，TTL 为 refresh token 有效期。
6. 返回：

```json
{
  "token": "<access-token>",
  "refreshToken": "<refresh-token>",
  "expiresIn": 1800,
  "refreshExpiresIn": 604800,
  "currentUser": {}
}
```

## 刷新接口

新增接口：

```http
POST /refresh-token
Content-Type: application/json

{
  "refreshToken": "<refresh-token>"
}
```

成功响应和登录响应保持同一结构，至少返回新的 access token；推荐同时轮换 refresh token。

刷新流程：

1. 校验 refresh token 格式并计算哈希。
2. 查找对应 `login_refresh_tokens:{refreshTokenId}`。
3. 校验：
   - refresh 会话存在；
   - 未过期；
   - 未撤销；
   - 哈希匹配。
4. 删除旧 `login_tokens:{oldTokenId}`。
5. 生成新的 `tokenId` 和 access token。
6. 写入新的 `login_tokens:{newTokenId}`。
7. 更新 refresh 会话中的 `currentTokenId`。
8. 刷新 `login_refresh_tokens:{refreshTokenId}` 与 `login_accounts:{accountId}` 的 TTL。
9. 返回新 access token。

## Refresh Token 轮换

推荐启用 refresh token rotation：

1. 每次刷新都返回新的 refresh token 明文。
2. 服务端用新哈希覆盖旧哈希。
3. 客户端收到响应后同时替换 access token 和 refresh token。
4. 如果旧 refresh token 再次被使用，视为重放风险，撤销该 refresh 会话并释放账号占用。

第一阶段如果要降低改造范围，可以不轮换 refresh token，但必须保证退出登录、监控强退、过期清理能撤销 refresh 会话。

## 并发控制

### 服务端

刷新同一个 refresh 会话时需要加锁，建议使用 Redis 锁：

```text
login_refresh_locks:{refreshTokenId}
```

锁 TTL 建议 5 到 10 秒。拿不到锁时可以返回“刷新中，请稍后重试”，或等待短时间后读取最新 refresh 会话状态。

### Web 前端

Web 端 axios 拦截器需要保持单飞刷新：

1. 收到 `106/107/108` 或 HTTP `401/403`。
2. 如果当前没有刷新任务，调用 `/refresh-token`。
3. 刷新期间其它失败请求进入队列。
4. 刷新成功后替换本地 token，并重放队列请求。
5. 刷新失败后清理本地登录态并跳转登录页。

### 桌面客户端

桌面客户端建议采用主动刷新：

1. 记录 access token 过期时间。
2. 在过期前 1 到 5 分钟调用 `/refresh-token`。
3. 如果调用 API 收到认证失败，再尝试一次刷新。
4. 刷新失败后进入未登录状态，提示用户重新登录。
5. refresh token 应存放在系统安全存储中，例如 Windows Credential Manager，而不是明文配置文件。

## 退出登录

`/logout` 应按当前 access token 找到 tokenId，再找到其 refresh 会话并撤销：

1. 删除 `login_tokens:{tokenId}`。
2. 删除或标记撤销 `login_refresh_tokens:{refreshTokenId}`。
3. 仅当 `login_accounts:{accountId}` 当前值等于该 `refreshTokenId` 时删除账号占用。

如果 access token 已过期，客户端仍可选择调用：

```http
POST /logout
Authorization: Bearer <expired-access-token>
```

但服务端解析过期 JWT 复杂度较高。更推荐新增：

```http
POST /logout-refresh-token

{
  "refreshToken": "<refresh-token>"
}
```

用于桌面客户端或 Web 端在 access token 已失效时主动释放 refresh 会话。

## 监控强退

在线用户列表应从 refresh 会话维度展示，而不是只展示短期 access token：

| 字段 | 来源 |
| --- | --- |
| tokenId | refresh 会话中的 `currentTokenId` |
| refreshTokenId | refresh 会话 ID |
| username | refresh 会话 |
| loginTime | `SystemLoginUser.loginInfo.loginTime` 或 refresh 会话 `issuedAt` |
| expiresAt | refresh 会话过期时间 |

强退时按 `refreshTokenId` 撤销 refresh 会话，并删除当前 `login_tokens:{currentTokenId}` 和账号占用。

## 配置项

建议新增：

```yaml
token:
  expirationSeconds: 1800
  refreshExpirationSeconds: 604800
  refreshRotationEnabled: true
  refreshLockSeconds: 10
```

说明：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `token.expirationSeconds` | `1800` | access token 有效期 |
| `token.refreshExpirationSeconds` | `604800` | refresh token 有效期 |
| `token.refreshRotationEnabled` | `true` | 是否每次刷新都轮换 refresh token |
| `token.refreshLockSeconds` | `10` | 单个 refresh 会话刷新锁过期时间 |

## 客户端协议

### Web

Web 登录后保存：

```ts
{
  token: string;
  refreshToken: string;
  expiresIn: number;
  refreshExpiresIn: number;
  currentUser: CurrentLoginUserDTO;
}
```

响应拦截器刷新成功后必须原子替换本地 token 数据。刷新失败时调用统一的本地会话清理逻辑并跳转 `/login`。

### 桌面客户端

桌面客户端登录后保存：

| 数据 | 保存建议 |
| --- | --- |
| access token | 内存优先，可短期落盘 |
| refresh token | 系统凭据库或应用自有加密存储 |
| access token 过期时间 | 本地配置或内存 |
| refresh token 过期时间 | 本地配置或内存 |

桌面客户端启动时，如果 refresh token 仍存在，可以先调用 `/refresh-token` 获取新的 access token，而不是要求用户重新输入密码。

## 错误码建议

新增业务或客户端错误码：

| 错误 | 建议提示 |
| --- | --- |
| refresh token 缺失 | `refresh token不能为空` |
| refresh token 无效 | `登录状态已失效，请重新登录` |
| refresh token 已过期 | `登录状态已过期，请重新登录` |
| refresh token 被撤销 | `登录状态已失效，请重新登录` |
| refresh token 重放 | `登录状态存在风险，请重新登录` |

前端和桌面客户端对这些错误统一进入重新登录流程。

## 迁移步骤

建议分阶段落地：

1. 后端扩展登录响应，新增 refresh token 字段。
2. 新增 Redis refresh 会话缓存和 `/refresh-token` 接口。
3. 调整 `login_accounts:{accountId}` 从绑定 `tokenId` 改为绑定 `refreshTokenId`。
4. 调整 `/logout` 和监控强退，撤销 refresh 会话。
5. Web 前端增加单飞刷新与请求重放。
6. 桌面客户端接入 refresh token 主动刷新。
7. 上线时清理旧 `login_tokens:*` 和 `login_accounts:*`，要求旧版本客户端重新登录一次。

## 测试覆盖

后端应覆盖：

1. 登录返回 access token 与 refresh token。
2. refresh token 有效时可刷新 access token。
3. refresh 后旧 access token 对应 `login_tokens` 被删除。
4. refresh 会话过期、撤销、哈希不匹配时刷新失败。
5. 同一账号已有 refresh 会话时拒绝第二次登录。
6. refresh 会话残留但已过期时允许重新登录。
7. `/logout` 同时删除 access token、refresh 会话和账号占用。
8. 监控强退按 refresh 会话释放账号占用。
9. 并发刷新同一个 refresh token 时不会产生多个有效 access token。

Web 前端应覆盖：

1. `106/107/108` 或 `401/403` 时触发刷新。
2. 多个并发失败请求只触发一次刷新。
3. 刷新成功后重放原请求。
4. 刷新失败后跳转登录页。

桌面客户端应覆盖：

1. access token 过期前主动刷新。
2. API 返回认证失败后补偿刷新一次。
3. refresh token 过期或被撤销时进入未登录状态。
