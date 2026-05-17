# Keystone 数据库与 Redis 密码加密使用说明

本文档说明如何在 Keystone 中使用统一 AES-256-GCM 密文格式配置数据库密码和 Redis 密码。

## 1. 功能概览

推荐密文格式：

```text
secret:v1:aes-256-gcm:<nonce_base64>:<ciphertext_base64>
```

Keystone 启动时会在对应开关开启后自动识别并解密该格式，然后注入数据源或 Spring Data Redis。

兼容说明：旧 `ENC(...)` AES/ECB 密文仍可解密，但不再推荐新部署使用。

支持的密码配置键：

- `spring.datasource.password`
- `spring.datasource.dynamic.datasource.<name>.password`
- `spring.datasource.password-file`
- `spring.datasource.dynamic.datasource.master.password-file`
- `spring.data.redis.password`
- `spring.data.redis.password-file`

## 2. 启用配置

在基础配置中已提供以下参数：

- `keystone.datasource.password-encryption.enabled`
- `keystone.datasource.password-encryption.encrypt-key`
- `keystone.datasource.password-encryption.encrypt-key-file`
- `keystone.redis.password-encryption.enabled`
- `keystone.redis.password-encryption.encrypt-key`
- `keystone.redis.password-encryption.encrypt-key-file`

推荐通过文件路径注入 key，而不是把 key 字符串写入 `.env`：

- `KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED=true`
- `KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE=/run/secrets/.database_password.key`
- `KEYSTONE_REDIS_PASSWORD_ENCRYPTION_ENABLED=true`
- `KEYSTONE_REDIS_ENCRYPT_KEY_FILE=/run/secrets/.redis_password.key`
- `SPRING_DATASOURCE_PASSWORD_FILE=/run/secrets/.database_password.enc`
- `SPRING_DATA_REDIS_PASSWORD_FILE=/run/secrets/.redis_password.enc`

注意：

- 如果密码配置为密文，但未提供 `encrypt-key` 或 `encrypt-key-file`，应用会在启动时报错并终止。
- 新格式要求密钥为 32 字节或标准 base64 编码后的 32 字节。

## 3. 生成密文

推荐使用仓库脚本一次生成 Docker 部署所需 secret：

```bash
python scripts/secret_tool.py generate-deployment --keep-database-plain
```

默认生成：

```text
docker/.secrets/.database_password
docker/.secrets/.database_password.enc
docker/.secrets/.database_password.key
docker/.secrets/.redis.acl
docker/.secrets/.redis_password.enc
docker/.secrets/.redis_password.key
```

如果需要自定义数据库密码，先手动创建 `docker/.secrets/.database_password` 并写入明文密码，再执行生成命令。脚本会优先使用该文件中的非空内容生成 `.database_password.enc` 和 `.database_password.key`。如果文件不存在或内容为空，脚本会生成一个包含字母、数字和特殊字符的强随机密码。

这里必须区分两个阶段：

- MySQL 或 PostgreSQL 等数据库容器首次初始化必须读取明文数据库密码。该密码由 `docker/.secrets/.database_password` 提供给容器初始化环境，这是官方镜像初始化数据库所需，无法用密文替代。
- Keystone 运行期不读取明文数据库密码，只读取 `docker/.secrets/.database_password.enc` 和 `docker/.secrets/.database_password.key`，在内存中解密后连接数据库。

因此首次启动内置数据库时使用 `--keep-database-plain` 保留 `.database_password`。确认数据库已经初始化成功后，应删除明文文件：

```bash
rm docker/.secrets/.database_password
```

Windows PowerShell：

```powershell
Remove-Item -LiteralPath docker/.secrets/.database_password
```

删除后不要对已有数据库数据卷重新执行初始化；后续只启动 Keystone 应用或使用已初始化的数据卷时，运行期只需要 `.database_password.enc` 和 `.database_password.key`。如果需要重新创建数据库数据卷，必须重新准备 `.database_password`，且明文内容必须与 `.database_password.enc` 中加密的密码一致。

Redis 不需要明文密码文件。Redis 容器读取 `.redis.acl`，其中只保存密码 SHA-256 hash；Keystone 运行期读取 `.redis_password.enc` 和 `.redis_password.key`。

也保留 Java 工具类：

- `app.keystone.infrastructure.security.DataSourcePasswordEncryptor`

用法：

```bash
java DataSourcePasswordEncryptor <encryptKey> <plainPassword>
```

输出示例：

```text
secret:v1:aes-256-gcm:...
```

也可以使用 Keylo 仓库的 `scripts/secret_tool.py` 或其他项目中的 `scripts/encrypt_db_password.py` 生成同一格式密文。

## 4. 配置示例

### 4.1 Docker Compose 环境变量

```yaml
KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED: true
KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE: /run/secrets/.database_password.key
SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/.database_password.enc
KEYSTONE_REDIS_PASSWORD_ENCRYPTION_ENABLED: true
KEYSTONE_REDIS_ENCRYPT_KEY_FILE: /run/secrets/.redis_password.key
SPRING_DATA_REDIS_PASSWORD_FILE: /run/secrets/.redis_password.enc
```

### 4.2 application-dev.yml / application-prod.yml

```yaml
spring:
  datasource:
    dynamic:
      datasource:
        master:
          password: ${SPRING_DATASOURCE_PASSWORD:12345}
          password-file: ${SPRING_DATASOURCE_PASSWORD_FILE:}
  data:
    redis:
      password: ${SPRING_DATA_REDIS_PASSWORD:12345}
      password-file: ${SPRING_DATA_REDIS_PASSWORD_FILE:}
```

## 5. 排错指南

1. 启动报错提示缺少密钥
   - 检查是否设置了 `KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE` 或 `KEYSTONE_REDIS_ENCRYPT_KEY_FILE`。
   - 检查 `KEYSTONE_DATASOURCE_PASSWORD_ENCRYPTION_ENABLED` 是否为 `true`。

2. 启动后数据库认证失败
   - 确认密钥与生成密文时使用的密钥完全一致。
   - 确认数据库或 Redis 实际密码与密文中的明文一致。

3. 解密失败
   - 确认密文没有被截断。
   - 确认密钥是 32 字节或标准 base64 编码后的 32 字节。
   - 确认密文格式以 `secret:v1:aes-256-gcm:` 开头。

## 6. 安全建议

- 不要把明文密码写入仓库。
- 密钥建议通过文件 secret 或部署平台 secret 注入，不要写在 `.env` 中。
- Redis ACL 文件只保存 SHA-256 hash，不保存明文密码。
- 定期轮换密钥、数据库密码和 Redis 密码。
