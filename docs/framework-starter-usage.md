# Keystone Framework Starter 使用说明

## Starter 示例项目

如果你想先看一个完整可运行的 starter 集成示例，请使用官方仓库：  
[https://github.com/bruceblink/my-keystone-starter-demo](https://github.com/bruceblink/my-keystone-starter-demo)

## 适用范围

`keystone-framework-spring-boot-starter` 是 Keystone 框架能力的聚合依赖，用于给 Spring Boot 应用提供系统管理、认证授权、基础设施配置、框架领域服务和系统数据库迁移。

当前 starter 聚合链路：

```text
keystone-framework-spring-boot-starter
└── keystone-framework-admin
    └── keystone-framework-domain
        └── keystone-infrastructure
            └── keystone-common
```

业务模块不属于 starter。具体业务代码和业务迁移仍由下游应用单独引入。

消费应用不需要再通过 `@ComponentScan("app.keystone.*")` 扫描整个仓库。引入 starter 后，框架 controller、领域服务和基础设施组件由自动配置加载；应用自己的包仍由本应用的 `@SpringBootApplication` 默认扫描范围负责。

## 环境要求

- Java 17+
- Spring Boot 3.5.x
- Gradle 或 Maven 消费内部制品
- MySQL 数据库
- Redis
- 已发布的 Keystone framework 相关制品

当前制品坐标：

```text
groupId: app.keystone
artifactId: keystone-framework-spring-boot-starter
version: 3.6.1
```

## 发布到本地 Maven 仓库

开发联调时先在 Keystone 仓库发布 framework 制品：

```powershell
.\gradlew.bat publishToMavenLocal
```

发布后本地 Maven 仓库应存在：

```text
~/.m2/repository/app/keystone/keystone-common/3.6.1
~/.m2/repository/app/keystone/keystone-infrastructure/3.6.1
~/.m2/repository/app/keystone/keystone-framework-domain/3.6.1
~/.m2/repository/app/keystone/keystone-framework-admin/3.6.1
~/.m2/repository/app/keystone/keystone-framework-spring-boot-starter/3.6.1
```

## Gradle 接入

业务应用使用本地 Maven 联调时：

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'app.keystone:keystone-framework-spring-boot-starter:3.6.1'
}
```

使用内部 Maven 仓库时，把 `mavenLocal()` 替换为内部仓库地址。

## 发布到远程 Maven 仓库

远程发布仓库、凭证和签名通过 Gradle 属性或环境变量注入，不写入源码仓库。

示例：

```powershell
.\gradlew.bat publish `
  -PkeystonePublishUrl=https://maven.example.com/releases `
  -PkeystonePublishUsername=$env:MAVEN_USERNAME `
  -PkeystonePublishPassword=$env:MAVEN_PASSWORD `
  -PsigningInMemoryKey="$env:SIGNING_IN_MEMORY_KEY" `
  -PsigningInMemoryKeyPassword="$env:SIGNING_IN_MEMORY_KEY_PASSWORD" `
  -PkeystoneSigningRequired=true
```

未配置 `keystonePublishUrl` 时，`publish` 会发布到 `build/maven-repository`，便于本地检查远程发布产物结构。

## Maven 接入

```xml
<dependency>
    <groupId>app.keystone</groupId>
    <artifactId>keystone-framework-spring-boot-starter</artifactId>
    <version>3.6.1</version>
</dependency>
```

## 应用配置

starter 中的 `application-basic.yml` 不会被 Spring Boot 自动加载。业务应用需要显式导入，或自行复制同等配置。

推荐：

```yaml
spring:
  config:
    import: "classpath:application-basic.yml"
```

最小配置示例：

```yaml
spring:
  config:
    import: "classpath:application-basic.yml"
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    dynamic:
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/keystone?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
          username: root
          password: root
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
    locations:
      - classpath:db/migrate/common
      - classpath:db/migrate/mysql

keystone:
  file-base-dir: ./data
  rsaPrivateKey: ${KEYSTONE_RSA_PRIVATE_KEY:}
  rate-limit:
    backend: redis
    fallback-to-local: false
```

## 数据库迁移

starter 只携带框架/系统迁移：

```text
classpath:db/migrate/common
classpath:db/migrate/mysql
```

这些迁移主要初始化 `sys_*` 系统表、系统字典、系统菜单等框架数据。

业务迁移不在 starter 中。下游应用如果有自己的业务表，应使用自己的迁移路径，例如：

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migrate/common
      - classpath:db/migrate/mysql
      - classpath:db/migrate/app/mysql
```

## 限流配置

框架接口通过 `@RateLimit` 声明限流规则。注解不选择 Redis 或本地内存，后端由全局配置控制：

```yaml
keystone:
  rate-limit:
    backend: redis
    fallback-to-local: false
```

`backend=redis` 是生产默认，适合多实例部署。`backend=local` 只建议用于本地开发或单实例场景。`fallback-to-local=false` 是生产推荐值，避免 Redis 故障时全局限流静默退化为每节点限流。

完整设计见 [Keystone 限流设计](rate-limit-design.md)。

## 自动配置开关

默认全部启用：

```yaml
keystone:
  framework:
    infrastructure:
      enabled: true
    domain:
      enabled: true
    admin:
      enabled: true
```

按需关闭：

```yaml
keystone:
  framework:
    admin:
      enabled: false
```

常见场景：

- 只复用基础设施，不暴露系统管理接口：关闭 `keystone.framework.admin.enabled`。
- 只调试业务数据库，不执行框架迁移：设置 `spring.flyway.enabled=false`。

## 当前自动装配内容

starter 通过 Spring Boot `AutoConfiguration.imports` 暴露：

```text
app.keystone.framework.admin.autoconfigure.KeystoneFrameworkAdminAutoConfiguration
app.keystone.framework.domain.autoconfigure.KeystoneFrameworkDomainAutoConfiguration
app.keystone.framework.domain.autoconfigure.KeystoneFrameworkInfrastructureAutoConfiguration
```

扫描的框架包：

```text
app.keystone.admin.controller.common
app.keystone.admin.controller.system
app.keystone.admin.customize
app.keystone.domain.common
app.keystone.domain.system
app.keystone.common.config
app.keystone.infrastructure
```

注意：当前仍保留历史包名，外部应用不要定义同名类或同路径控制器，避免 Bean 或路由冲突。

自动配置包含系统管理 Web 层，例如用户、角色、菜单、日志、监控、定时任务等 controller。Keystone 主应用已经删除 `keystone-admin` 中重复的系统监控 controller，`/monitor/**` 由 `keystone-framework-admin` 提供。

## Keystone 主应用使用方式

Keystone 主应用已经接入 starter：

```gradle
dependencies {
    implementation project(':keystone-framework-spring-boot-starter')
    implementation project(':keystone-domain')
}
```

启动类只保留 Spring Boot 默认扫描：

```java
@SpringBootApplication
public class KeystoneAdminApplication {
}
```

不要在主应用中重新添加 `@ComponentScan(basePackages = "app.keystone.*")`。全仓扫描会掩盖 starter 自动配置问题，也可能把下游扩展模块中尚未明确暴露的组件误装配进运行时。

主应用加载框架迁移：

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migrate/common
      - classpath:db/migrate/mysql
```

## 验证命令

本地验证 starter：

```powershell
.\gradlew.bat :keystone-framework-spring-boot-starter:test
```

验证 starter 自动配置和主应用接入：

```powershell
.\gradlew.bat :keystone-framework-spring-boot-starter:test :keystone-framework-domain:test :keystone-framework-admin:test :keystone-admin:test :keystone-admin:compileJava
```

验证发布：

```powershell
.\gradlew.bat :keystone-common:publishToMavenLocal :keystone-infrastructure:publishToMavenLocal :keystone-framework-domain:publishToMavenLocal :keystone-framework-admin:publishToMavenLocal :keystone-framework-spring-boot-starter:publishToMavenLocal
```

验证 Keystone 主应用：

```powershell
.\gradlew.bat :keystone-admin:test :keystone-domain:test :keystone-admin:compileJava
```

## 新项目快速模板（Gradle）

下面这套最小模板可直接用于新项目，适合先跑通“starter 引入 + 框架自动配置 + 系统迁移”链路。按你的项目名替换目录和包名即可。

### 目录结构

```text
my-keystone-app/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── docker-compose.yml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── your
    │   │           └── app
    │   │               └── AppApplication.java
    │   └── resources
    │       └── application.yml
    └── test
        └── java
            └── com
                └── your
                    └── app
                        └── AppApplicationTests.java
```

业务 SQL 放在：

```text
src/main/resources/db/migrate/app/mysql
```

### settings.gradle

```gradle
rootProject.name = 'my-keystone-app'
```

### build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.13'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.your.app'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    mavenLocal() // 开发调试用，发布后可替换为内部仓库
}

dependencies {
    implementation 'app.keystone:keystone-framework-spring-boot-starter:3.6.1'
    runtimeOnly 'com.mysql:mysql-connector-j'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2g
org.gradle.parallel=true
org.gradle.caching=true
```

### src/main/resources/application.yml

```yaml
spring:
  application:
    name: my-keystone-app
  config:
    import: "classpath:application-basic.yml"
  datasource:
    dynamic:
      datasource:
        master:
          url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/my_keystone_app?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
          username: ${SPRING_DATASOURCE_USERNAME:root}
          password-file: ${SPRING_DATASOURCE_PASSWORD_FILE:}
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:localhost}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      database: ${SPRING_DATA_REDIS_DATABASE:0}
      password-file: ${SPRING_DATA_REDIS_PASSWORD_FILE:}
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: true
    locations:
      - classpath:db/migrate/common
      - classpath:db/migrate/mysql
      - classpath:db/migrate/app/mysql

keystone:
  framework:
    infrastructure:
      enabled: true
    domain:
      enabled: true
    admin:
      enabled: true
  datasource:
    password-encryption:
      encrypt-key-file: ${KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE:}
  redis:
    password-encryption:
      encrypt-key-file: ${KEYSTONE_REDIS_ENCRYPT_KEY_FILE:}

  rsaPrivateKey: ${KEYSTONE_RSA_PRIVATE_KEY:}
  file-base-dir: ./data
```

说明：

- 如果你按 `secret:v1` 文件解密流程，`password-file` 和对应的 `*-encrypt-key-file` 都要补齐。
- starter 不会携带业务迁移，业务表在 `db/migrate/app/mysql` 下管理。

### 主应用启动类

```java
package com.your.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class AppApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(AppApplication.class).run(args);
    }
}
```

### docker-compose.yml

```yaml
version: "3.9"
services:
  mysql:
    image: mysql:8.4
    container_name: my-app-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:root}
      MYSQL_DATABASE: my_keystone_app
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:8.6.2-alpine
    container_name: my-app-redis
    command: redis-server --save "" --appendonly no
    ports:
      - "6379:6379"

  app:
    image: eclipse-temurin:21-jdk
    container_name: my-keystone-app
    depends_on:
      - mysql
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/my_keystone_app?useUnicode=true&characterEncoding=utf8&connectionCollation=utf8mb4_unicode_ci&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD_FILE: /run/secrets/.database_password.enc
      KEYSTONE_DATASOURCE_ENCRYPT_KEY_FILE: /run/secrets/.database_password.key
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD_FILE: /run/secrets/.redis_password.enc
      KEYSTONE_REDIS_ENCRYPT_KEY_FILE: /run/secrets/.redis_password.key
      KEYSTONE_RSA_PRIVATE_KEY: ${KEYSTONE_RSA_PRIVATE_KEY}
    volumes:
      - ./build/libs/my-keystone-app.jar:/app/my-keystone-app.jar
      - ./docker-secrets/.database_password.enc:/run/secrets/.database_password.enc:ro
      - ./docker-secrets/.database_password.key:/run/secrets/.database_password.key:ro
      - ./docker-secrets/.redis_password.enc:/run/secrets/.redis_password.enc:ro
      - ./docker-secrets/.redis_password.key:/run/secrets/.redis_password.key:ro
    command: ["java", "-jar", "/app/my-keystone-app.jar"]
    ports:
      - "18080:18080"

volumes:
  mysql_data:
```

### Flyway 业务迁移示例

```text
src/main/resources/db/migrate/app/mysql/V1__init_my_app_tables.sql
```

```sql
CREATE TABLE IF NOT EXISTS app_sample
(
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_unicode_ci;
```

### 一次性启动步骤

```powershell
.\gradlew.bat bootJar
docker compose up -d
```

如果没有提前打包，可本地运行：

```powershell
.\gradlew.bat bootRun
```

`docker-secrets/` 目录建议与项目一同维护，用于本地启动时挂载：

```text
docker-secrets/
├── .database_password.enc
├── .database_password.key
├── .redis_password.enc
└── .redis_password.key
```

## 常见问题

### 引入 starter 后没有加载基础配置

检查应用是否配置：

```yaml
spring:
  config:
    import: "classpath:application-basic.yml"
```

### Flyway 找不到迁移脚本

检查 `spring.flyway.locations` 是否包含：

```text
classpath:db/migrate/common
classpath:db/migrate/mysql
```

### 业务表没有创建

starter 不携带业务迁移。业务应用需要额外提供自己的迁移路径，例如：

```text
classpath:db/migrate/app/mysql
```

### 不想暴露系统管理接口

关闭 admin 自动配置：

```yaml
keystone:
  framework:
    admin:
      enabled: false
```

### 引入 starter 后系统接口 404

检查应用是否只引入了 `keystone-framework-spring-boot-starter`，而不是只引入了 `keystone-common` 或 `keystone-infrastructure`。还需要确认没有关闭：

```yaml
keystone:
  framework:
    domain:
      enabled: true
    admin:
      enabled: true
```
