# Keystone 自动刷新 token 设计

## 背景

Keystone 当前使用短期 JWT 作为访问令牌，JWT 中保存 `login_user_key={tokenId}`，真实登录用户信息缓存在 Redis `login_tokens:{tokenId}` 中。为了保证单账号登录状态一致，Redis 登录态 TTL 已调整为和 `token.expirationSeconds` 一致。

这能解决 Web 端 token 过期后账号占用残留的问题，但对桌面客户端不够友好。桌面客户端通常需要长时间运行，不能因为 access token 到期就频繁要求用户重新输入账号密码。因此需要引入完整的 refresh token 机制，而不是恢复“只刷新 Redis、不刷新 JWT”的旧逻辑。

## 术语表

| 术语 | 说明 |
| --- | --- |
| access token | 客户端调用 Keystone API 时携带的短期访问令牌。当前实现中它是 JWT，放在 `Authorization: Bearer <token>` 请求头中。 |
| refresh token | 客户端用于换取新 access token 的长期凭证。它不直接用于调用业务 API，只用于 `/refresh-token`。 |
| JWT | JSON Web Token。Keystone 的 access token 使用 JWT 格式，服务端可以校验签名、读取过期时间和 `login_user_key` 等 claim。 |
| claim | JWT 中保存的键值数据，例如 `login_user_key`、`login_user_id`、`exp`。 |
| tokenId | Keystone 生成的 access token 会话 ID，保存在 JWT 的 `login_user_key` claim 中，也是 Redis `login_tokens:{tokenId}` 的后缀。 |
| refreshTokenId | Keystone 生成的 refresh 会话 ID，用于定位 Redis 中的 `login_refresh_tokens:{refreshTokenId}`。它不是 refresh token 明文。 |
| refresh token 明文 | 返回给客户端保存的随机字符串。客户端刷新时提交它；服务端不应在 Redis 中保存明文。 |
| refreshTokenHash | refresh token 明文经过哈希后的值，服务端保存在 Redis 中，用于刷新时比对。 |
| refresh 会话 | 服务端保存的一条长期登录状态记录，Redis key 为 `login_refresh_tokens:{refreshTokenId}`。它把 refresh token、当前 access token、账号占用、过期时间和撤销状态关联起来。 |
| accountId | 单账号登录占用 ID。优先使用 `sys_user.user_id`，缺失时使用 `username` 兜底。 |
| login session | Keystone 登录会话。本文中指一个 refresh token 会话以及它当前关联的 access token。 |
| 单账号占用 | Redis 中 `login_accounts:{accountId}` 记录某账号当前在线会话，用于拒绝第二次登录。 |
| token rotation | refresh token 轮换。每次刷新时服务端返回新的 refresh token，并使旧 refresh token 失效。 |
| token replay | refresh token 重放。旧 refresh token 已被轮换或撤销后再次被使用，通常表示客户端并发异常或凭证泄露风险。 |
| 单飞刷新 | 客户端并发请求同时遇到 token 失效时，只发起一次 `/refresh-token`，其它请求等待刷新结果。 |

## 核心概念

### access token 与 refresh token 的区别

access token 是“访问 API 的临时通行证”，有效期短，泄露后的风险窗口也短。它会被频繁放进请求头，因此不适合设计得太长。

refresh token 是“重新获取 access token 的长期凭证”，有效期更长，使用频率更低，必须更谨慎保存。它不应该出现在普通业务 API 请求中，也不应该写入日志。

### tokenId 与 refreshTokenId 的区别

`tokenId` 表示一次短期 access token 会话。每次刷新 access token 都会生成新的 `tokenId`，旧的 `login_tokens:{oldTokenId}` 应被删除。

`refreshTokenId` 表示一次长期登录会话。只要用户没有退出、没有被强退、refresh token 没有过期，这个 ID 可以在多次 access token 刷新之间保持稳定。

文档中提到的“refresh 会话”就是 `refreshTokenId` 对应的服务端状态对象。它不是客户端手里的 refresh token 明文，而是服务端用来管理长期登录状态的记录。

因此：

```text
一个 refreshTokenId
  -> 同一时间只关联一个 currentTokenId
  -> 每次刷新都会替换 currentTokenId
```

### 单账号占用为什么绑定 refreshTokenId

如果 `login_accounts:{accountId}` 绑定短期 `tokenId`，access token 刷新后账号占用也必须频繁更新，容易出现旧 token 已删、新 token 未写入之间的短暂不一致。

绑定 `refreshTokenId` 更稳定，因为它代表完整登录会话，而不是某一次短期 access token。只要 refresh 会话有效，就认为该账号在线。

### 为什么 refresh token 不直接用 JWT

refresh token 可以使用 JWT，但 Keystone 更适合使用随机字符串加服务端 Redis 状态：

1. 服务端可以立即撤销 refresh token。
2. 服务端可以检测 token rotation 后的重放。
3. Redis 中只保存哈希，即使 Redis 泄露也不能直接拿来刷新。
4. 单账号登录状态天然需要服务端状态，使用随机 refresh token 更直接。

### 过期和撤销的区别

过期是自然失效，由 TTL 或 `expiresAt` 控制。撤销是人为失效，例如用户主动退出、管理员强退、检测到 refresh token 重放。

客户端不需要区分两者的 UI 行为：都应进入重新登录流程。但服务端日志应区分，以便排查安全问题和用户行为。

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

access token 的有效期决定“单次 API 访问凭证多久失效”。refresh token 的有效期决定“用户最多可以保持登录多久”。例如 access token 30 分钟、refresh token 7 天，表示客户端最多每 30 分钟需要刷新一次，但 7 天内无需重新输入账号密码。

## Refresh 会话有效期策略

refresh 会话的有效期和 refresh token 有效期绑定，由 `token.refreshExpirationSeconds` 控制。默认值建议为 7 天：

```yaml
token:
  refreshExpirationSeconds: 604800
```

默认策略是固定有效期：用户从登录成功开始最多保持登录 7 天。期间即使客户端持续刷新 access token，refresh 会话的最终过期时间也不变。到达 `expiresAt` 后，refresh token 失效，客户端必须重新登录。

固定有效期下：

| 对象 | 有效期来源 | 是否随刷新延长 |
| --- | --- | --- |
| `login_tokens:{tokenId}` | `token.expirationSeconds` | 每次刷新 access token 都生成新 key 和新 TTL |
| `login_refresh_tokens:{refreshTokenId}` | `token.refreshExpirationSeconds` 和固定 `expiresAt` | 否 |
| `login_accounts:{accountId}` | 与 refresh 会话一致 | 否 |

也允许配置为滚动刷新 refresh token。启用后，每次 `/refresh-token` 成功都会把 refresh 会话有效期向后延长一个 `token.refreshExpirationSeconds` 周期，适合需要“只要持续使用就不掉线”的桌面客户端场景。

建议配置项：

```yaml
token:
  refreshSlidingExpirationEnabled: false
```

滚动刷新开启后：

| 对象 | 有效期来源 | 是否随刷新延长 |
| --- | --- | --- |
| `login_tokens:{tokenId}` | `token.expirationSeconds` | 是，生成新 access token 时重建 |
| `login_refresh_tokens:{refreshTokenId}` | `now + token.refreshExpirationSeconds` | 是 |
| `login_accounts:{accountId}` | 与 refresh 会话一致 | 是 |

无论是否启用滚动刷新，服务端都必须在 refresh 会话对象中保存明确的 `expiresAt`。Redis TTL 只是自动清理手段，业务判断应以 `expiresAt` 和 `revoked` 为准。

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

三类 key 的关系：

```text
login_accounts:{accountId}
  -> refreshTokenId

login_refresh_tokens:{refreshTokenId}
  -> currentTokenId
  -> refreshTokenHash
  -> accountId

login_tokens:{currentTokenId}
  -> SystemLoginUser
```

服务端判断账号是否在线时，应以 `login_accounts:{accountId}` 指向的 refresh 会话是否有效为准。服务端处理 API 请求时，应以 access token 中的 `tokenId` 是否能查到 `login_tokens:{tokenId}` 为准。

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

刷新接口不需要携带有效 access token。原因是调用刷新接口时，access token 可能已经过期。刷新接口的身份依据是 refresh token 本身。

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

如果启用轮换，客户端必须保证“保存新 token”是原子操作：收到刷新响应后，access token 和 refresh token 要一起替换。如果只替换了 access token，没有替换 refresh token，下一次刷新会使用旧 refresh token，服务端可能判断为重放。

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

Web 端不建议每个请求都主动检查过期时间。更简单的策略是：请求失败后由拦截器统一刷新。但如果页面存在长轮询、大文件上传、后台定时任务，也可以在请求前发现 access token 快过期时主动刷新。

### 桌面客户端

桌面客户端建议采用主动刷新：

1. 记录 access token 过期时间。
2. 在过期前 1 到 5 分钟调用 `/refresh-token`。
3. 如果调用 API 收到认证失败，再尝试一次刷新。
4. 刷新失败后进入未登录状态，提示用户重新登录。
5. refresh token 应存放在系统安全存储中，例如 Windows Credential Manager，而不是明文配置文件。

桌面客户端建议采用主动刷新，是因为它可能长时间没有用户交互，等 API 报错后再刷新会影响后台任务。主动刷新失败时，客户端应暂停需要认证的任务，并把状态切换为“需要重新登录”。

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

两种退出接口的职责边界：

| 接口 | 适用场景 | 身份依据 |
| --- | --- | --- |
| `/logout` | access token 仍有效时正常退出 | `Authorization` 中的 access token |
| `/logout-refresh-token` | access token 已失效但客户端仍持有 refresh token | 请求体中的 refresh token |

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
  refreshSlidingExpirationEnabled: false
  refreshLockSeconds: 10
```

说明：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `token.expirationSeconds` | `1800` | access token 有效期 |
| `token.refreshExpirationSeconds` | `604800` | refresh token / refresh 会话有效期，默认 7 天 |
| `token.refreshRotationEnabled` | `true` | 是否每次刷新都轮换 refresh token |
| `token.refreshSlidingExpirationEnabled` | `false` | 是否在每次刷新成功后滚动延长 refresh 会话有效期 |
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

Web 端保存 refresh token 有 XSS 风险。当前前端已经使用 JS 管理 token，因此第一阶段可以继续沿用现有存储方式；后续如要进一步加固，可以评估 HttpOnly Cookie，但这会影响跨域、CSRF、防重放等配套设计。

### 桌面客户端

桌面客户端登录后保存：

| 数据 | 保存建议 |
| --- | --- |
| access token | 内存优先，可短期落盘 |
| refresh token | 系统凭据库或应用自有加密存储 |
| access token 过期时间 | 本地配置或内存 |
| refresh token 过期时间 | 本地配置或内存 |

桌面客户端启动时，如果 refresh token 仍存在，可以先调用 `/refresh-token` 获取新的 access token，而不是要求用户重新输入密码。

桌面客户端不要把 refresh token 写入普通日志、崩溃报告、调试输出或明文配置文件。刷新失败时也不要把 refresh token 拼进错误消息。

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
