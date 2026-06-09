# Keystone Framework Starter 使用说明

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
.\gradlew.bat :keystone-common:publishToMavenLocal :keystone-infrastructure:publishToMavenLocal :keystone-framework-domain:publishToMavenLocal :keystone-framework-admin:publishToMavenLocal :keystone-framework-spring-boot-starter:publishToMavenLocal
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
  auth:
    mode: ${KEYSTONE_AUTH_MODE:local}
    keylo:
      enabled: ${KEYSTONE_AUTH_KEYLO_ENABLED:false}
```

## 数据库迁移

starter 只携带框架/系统迁移：

```text
classpath:db/migrate/common
classpath:db/migrate/mysql
```

这些迁移主要初始化 `sys_*` 系统表、系统字典、系统菜单、服务客户端等框架数据。

业务迁移不在 starter 中。下游应用如果有自己的业务表，应使用自己的迁移路径，例如：

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migrate/common
      - classpath:db/migrate/mysql
      - classpath:db/migrate/app/mysql
```

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

## Keystone 主应用使用方式

Keystone 主应用已经接入 starter：

```gradle
dependencies {
    implementation project(':keystone-framework-spring-boot-starter')
    implementation project(':keystone-domain')
}
```

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

验证发布：

```powershell
.\gradlew.bat :keystone-common:publishToMavenLocal :keystone-infrastructure:publishToMavenLocal :keystone-framework-domain:publishToMavenLocal :keystone-framework-admin:publishToMavenLocal :keystone-framework-spring-boot-starter:publishToMavenLocal
```

验证 Keystone 主应用：

```powershell
.\gradlew.bat :keystone-admin:test :keystone-domain:test :keystone-admin:compileJava
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
