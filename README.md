# Keystone

[![Release](https://img.shields.io/badge/Release-V3.5.0-green.svg)](https://github.com/bruceblink/Keystone)
[![JDK](https://img.shields.io/badge/JDK-25-green.svg)](https://github.com/bruceblink/Keystone)
[![Spring%20Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-blue.svg)](https://github.com/bruceblink/Keystone)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![CI](https://img.shields.io/badge/tests-passing-success)](https://github.com/bruceblink/Keystone)

> 一个面向中小团队与个人开发者的 **高质量 Java 后端脚手架**。  
> 基于 Spring Boot 3.5 + Vue3 生态，开箱即用、结构清晰、测试完善。

## ✨ 为什么选择 Keystone

- 🚀 **上手快**：模块化清晰，业务代码可快速落地
- 🔐 **安全默认**：Spring Security + JWT 认证授权，敏感配置强制环境变量注入
- 🧩 **可扩展**：分层架构，便于二开和团队协作
- 🧪 **质量可控**：单元测试 + 集成测试覆盖关键链路（136 个测试全部通过）
- 🛠️ **工程友好**：支持 Docker 本地环境，文档完善
- 📚 **数据字典**：内置字典类型 + 字典数据管理，支持 Redis 缓存

## 🧱 技术亮点

- **JDK 25**
- **Spring Boot 3.5.13**
- MyBatis-Plus / Dynamic Datasource / Druid
- **Caffeine** 本地高性能缓存 + Redis 分布式缓存（三级缓存架构）
- springdoc-openapi（Swagger UI）
- JUnit 5 + Mockito + JaCoCo
- Gradle Configuration Cache，增量构建可低至 1s

## 📦 模块结构

```text
keystone-admin           # 管理后台接口
keystone-common          # 通用基础能力
keystone-infrastructure  # 配置与基础设施
keystone-domain          # 核心业务领域
```

## ⚡ 30 秒开始

```bash
git clone https://github.com/bruceblink/Keystone
cd Keystone
./gradlew clean build -x test
./gradlew test
```

启动类：`app.keystone.admin.KeystoneAdminApplication`

## 🌍 在线体验

前端演示地址：<https://agileboot-front-end.pages.dev>

## 🐳 Docker 开发环境

### 服务组成

| 服务 | 镜像 | 容器名 | 宿主机端口 |
|------|------|--------|-----------|
| MySQL 8.4 | `mysql:8.4` | `infra_mysql` | `33066` |
| Redis 8.6.2 | `redis:8.6.2-alpine` | `infra_redis` | `6379` |
| Spring Boot 后端 | `keystone:latest` | `sys_manage_backend` | `18080` |

> 所有服务时区均为 **UTC**。

### 快速启动

#### 1. 仅启动中间件（MySQL + Redis，本地 IDE 运行应用）

```bash
cd docker
docker compose up -d mysql redis
```

#### 2. 启动完整环境（含构建应用镜像）

```bash
cd docker
docker compose up -d --build
```

#### 3. 使用本地已编译 jar 构建运行镜像

如果你已经在本地完成打包，希望 Docker 镜像直接使用本地生成的 `keystone-admin/build/libs/keystone-admin.jar`，可以使用 [Dockerfile.local-jar](Dockerfile.local-jar)：

```bash
./gradlew :keystone-admin:bootJar
docker build -f Dockerfile.local-jar -t keystone-admin:local-jar .
```

这个 Dockerfile **不会在镜像构建阶段再次执行 Gradle 编译**，而是直接复制本地已生成的 jar 包进入镜像。适合以下场景：

- 已在本地验证过 jar，希望快速重建运行镜像
- 远程服务器仅接收本地构建产物，不在镜像里重复编译
- 排查“代码变更 vs 镜像构建缓存”问题时，明确使用当前本地产物

使用前请确认本地存在：

- `keystone-admin/build/libs/keystone-admin.jar`

#### 4. 查看日志

```bash
# 所有服务
docker compose logs -f

# 单个服务
docker compose logs -f sys_manage_backend
docker compose logs -f mysql
```

### 环境变量

默认模板在 `docker/.env.example` 中。首次使用时复制为本地 `docker/.env`：

```bash
cd docker
cp .env.example .env
# 编辑 .env，按需修改密码、端口、Keylo 配置等
```

当前约定：

- Docker 运行时统一使用 `SPRING_DATA_REDIS_*`，不再使用旧的 `SPRING_REDIS_*`
- `KEYSTONE_AUTH_MODE` / `KEYSTONE_AUTH_KEYLO_ENABLED` 控制后端登录模式
- 当 Keylo token 的 `iss` 与服务访问地址不一致时，使用 `KEYLO_TRUSTED_ISSUERS` 显式指定可信 issuer，例如 `keylo`
- `KEYLO_LEGACY_TOKEN_LOGIN_ENABLED=false` 可关闭兼容保留的 `/login/keylo`
- `SPRING_PROFILES_ACTIVE` 默认为 `prod`，容器运行时走部署配置

#### 生成登录 RSA 密钥对

后端 `/login` 使用 RSA 私钥解密密码，建议统一使用仓库内的 Python 脚本生成密钥对：

```bash
python scripts/secret_tool.py generate-rsa
```

可选指定长度：

```bash
python scripts/secret_tool.py generate-rsa --bits 3072
```

输出说明：

- `KEYSTONE_RSA_PRIVATE_KEY`：配置到后端环境变量，格式为 `PKCS#8 DER + Base64`
- `KEYSTONE_RSA_PUBLIC_KEY`：对应公钥，格式为 `X.509 DER + Base64`

当前后端实际必须配置的是 `KEYSTONE_RSA_PRIVATE_KEY`。公钥可通过 `/login/rsa-public-key` 接口提供给前端动态获取。

### 挂载自定义 application.yml

容器会额外读取 `/app/config/` 目录下的 Spring Boot 配置文件：

- 如果宿主机 `docker/app/config/application.yml` 或同 profile 配置存在，则优先覆盖镜像内置默认配置
- 如果该目录为空，则继续使用镜像内置配置

示例：

```bash
mkdir -p docker/app/config
# 将自定义配置放到 docker/app/config/application-prod.yml
docker compose up -d
```

### 停止 / 销毁

```bash
# 停止（保留 volume 数据）
docker compose down

# 停止并删除所有数据（慎用）
docker compose down -v
```

### 本地 IDE 开发时的连接配置

`application-dev.yml` 默认使用本地开发配置：

- MySQL: `localhost:3306` / DB: `keystone` / User: `root` / Pass: `12345`
- Redis: `localhost:6379` / Pass: `12345`
- Profile: `dev`（通过 `spring.profiles.group` 自动附带 `basic`）

如需启用 Keylo 登录，请显式设置：

- `KEYSTONE_AUTH_MODE=mixed`
- `KEYSTONE_AUTH_KEYLO_ENABLED=true`

## 📚 文档入口

- 完整工程文档：[文档说明.md](文档说明.md)
- Docker 启动指南：见本文档上方“Docker 开发环境”章节
- 数据库脚本：
  - [keystone-infrastructure/src/main/resources/db/migrate/common/V3_3_0__flyway_baseline_marker.sql](keystone-infrastructure/src/main/resources/db/migrate/common/V3_3_0__flyway_baseline_marker.sql) — Flyway 基线标记
  - [keystone-infrastructure/src/main/resources/db/migrate/mysql/V3_3_1__init_core_schema_data.sql](keystone-infrastructure/src/main/resources/db/migrate/mysql/V3_3_1__init_core_schema_data.sql) — 核心结构与初始化数据
  - [keystone-infrastructure/src/main/resources/db/migrate/mysql/V3_3_2__init_dict_schema_data.sql](keystone-infrastructure/src/main/resources/db/migrate/mysql/V3_3_2__init_dict_schema_data.sql) — 字典结构与初始化数据
  - [keystone-infrastructure/src/main/resources/db/migrate/h2/keystone_schema.sql](keystone-infrastructure/src/main/resources/db/migrate/h2/keystone_schema.sql) — H2 测试 schema
  - [keystone-infrastructure/src/main/resources/db/migrate/h2/keystone_data.sql](keystone-infrastructure/src/main/resources/db/migrate/h2/keystone_data.sql) — H2 测试 data
- Flyway SQL 命名规范：
  - MySQL 迁移脚本统一放在 `keystone-infrastructure/src/main/resources/db/migrate/mysql/`
  - 文件名格式必须为 `V<版本号>__<描述>.sql`，例如 `V3_4_0__add_user_profile_table.sql`
  - 版本号使用递增语义版本风格，当前仓库约定使用下划线分段：`V3_4_0`、`V3_4_1`
  - `__` 前后不能省略；同一版本号不能重复
  - 描述部分使用英文小写加下划线，表达本次变更目的，如 `init_order_schema`、`add_user_email_index`
  - 已执行过的 Flyway 脚本不要重命名、不要改版本号；如需继续演进，新增更高版本脚本
  - 脚本内容不得写死数据库名（例如 `use keystone;`），应始终作用于当前 datasource 指向的库
- 数据库密码加密：[DATABASE_PASSWORD_ENCRYPTION_GUIDE.md](DATABASE_PASSWORD_ENCRYPTION_GUIDE.md)
- Keylo 对接说明（含可选启用、统一 `/login` 后端 Keylo 凭证鉴权、用户新增同步注册）：见 [文档说明.md](文档说明.md) 的“Keylo 集成与用户注册流程”章节

## 🤝 贡献

欢迎提 Issue / PR，一起把 Keystone 做得更好。

提交信息统一使用 Conventional Commits 风格：`type: subject` 或 `type(scope): subject`。

允许的 `type`：`build`、`chore`、`ci`、`docs`、`feat`、`fix`、`perf`、`refactor`、`release`、`revert`、`style`、`test`。

示例：

```text
feat(auth): add refresh token rotation
fix: handle null query parameter in user listing
docs(readme): update quick start commands
```

启用仓库内校验钩子：

```bash
git config core.hooksPath .githooks
```

如果这个项目对你有帮助，欢迎点个 **Star** ⭐
