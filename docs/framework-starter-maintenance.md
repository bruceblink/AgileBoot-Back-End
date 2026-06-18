# Keystone Framework Starter 维护文档

## 维护目标

`keystone-framework-spring-boot-starter` 的维护目标是让框架能力可以作为独立依赖被业务应用消费，同时避免把 Keystone 具体业务能力一起带入外部应用。

维护时重点保证：

- starter 可发布。
- starter 可被外部应用作为单一依赖引入。
- starter 自动配置可加载。
- starter 传递依赖不携带下游应用的业务迁移。
- Keystone 主应用仍可通过额外扩展模块获得应用能力。

## 模块边界

### keystone-common

保留通用 DTO、枚举、工具、基础实体、通用配置属性。

允许依赖：

- Spring 基础库
- Jackson
- MyBatis Plus API
- 通用工具库

不应引入：

- 下游应用业务领域服务
- 业务迁移脚本
- Web controller

### keystone-infrastructure

保留基础设施能力：

- 数据源、Redis、Jackson、MyBatis、事务配置
- 安全、登录用户上下文、线程池
- 全局异常、过滤器、限流、防重复提交
- 框架/系统迁移资源

允许保留迁移路径：

```text
src/main/resources/db/migrate/common
src/main/resources/db/migrate/mysql
```

这里的 `mysql` 目录只放系统/框架迁移，例如 `sys_*` 表、系统菜单、系统字典、服务客户端。

不应放入：

- 下游应用业务表迁移
- 测试专用 schema/data
- 业务初始化数据

### keystone-framework-domain

保留框架领域服务：

- `domain/common`
- `domain/system`
- 系统用户、角色、菜单、部门、岗位、字典、日志、通知、定时任务等框架领域能力
- 系统定时任务运行时、运行日志清理任务和相关单元测试

自动配置：

```text
KeystoneFrameworkInfrastructureAutoConfiguration
KeystoneFrameworkDomainAutoConfiguration
```

不应依赖：

- `keystone-domain`
- 下游应用业务领域包

### keystone-framework-admin

保留框架 Web 层：

- `admin/controller/common`
- `admin/controller/system`
- `admin/customize`
- 系统监控、日志、用户、角色、菜单、定时任务等框架 controller

自动配置：

```text
KeystoneFrameworkAdminAutoConfiguration
```

不应依赖：

- `keystone-admin`
- 业务 controller
- 业务 application service

`keystone-admin` 不应再保留同路径的框架 controller 副本。例如 `/monitor/**` 由 `keystone-framework-admin` 提供，主应用中不要再新增 `app.keystone.admin.controller.system.MonitorController`。

### keystone-framework-spring-boot-starter

只做依赖聚合，原则上不放业务代码。

当前依赖：

```gradle
dependencies {
    api project(':keystone-framework-admin')
}
```

starter 模块可以放少量测试，用于验证依赖链、自动配置和资源隔离。

### keystone-domain

保留为下游扩展模块。开源分支不在该模块内放置业务领域代码或业务迁移资源，也不保留空的主源码目录。

框架领域代码、框架定时任务和框架单元测试必须放在 `keystone-framework-domain`。如果下游应用需要自己的业务能力，应在消费应用自己的领域模块中创建源码包和迁移路径。

### keystone-admin

保留 Keystone 可执行应用入口。

主应用依赖：

```gradle
implementation project(':keystone-framework-spring-boot-starter')
implementation project(':keystone-domain')
```

主应用 Flyway 路径：

```text
classpath:db/migrate/common
classpath:db/migrate/mysql
```

主应用启动类只保留 `@SpringBootApplication`。不要重新添加 `@ComponentScan(basePackages = "app.keystone.*")`，否则会绕过 starter 自动配置边界，导致重复 controller 或隐藏自动配置缺失问题。

## 自动配置维护规则

自动配置注册文件：

```text
keystone-framework-admin/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
keystone-framework-domain/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

新增自动配置时必须：

1. 使用 `@AutoConfiguration`。
2. 放入对应模块的 `AutoConfiguration.imports`。
3. 增加测试确认 imports 文件包含该类。
4. 增加必要的 `@ConditionalOnProperty` 开关。

当前开关：

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

## 系统导出维护规则

系统管理导出接口必须使用无分页查询方法，不能复用分页列表结果。分页列表只返回当前页，导出接口需要导出筛选条件下的完整集合。

当前约定：

```text
SysUserController.exportUserByExcel -> UserApplicationService.exportUsers
SysRoleController.export -> RoleApplicationService.exportRoles
SysLogsController.loginInfosExcel -> LogApplicationService.exportLoginInfos
SysLogsController.operationLogsExcel -> LogApplicationService.exportOperationLogs
ConfigApplicationService.exportConfigs
```

维护要求：

- 新增系统导出接口时，在 application service 中提供明确的 `export...` 方法。
- `export...` 方法使用 `list(...)` 或专门的无分页 mapper 方法，不调用 `page(...)`。
- 需要稳定排序的日志类导出必须补充主排序字段之外的 ID 倒序，例如 `login_time, info_id`。
- 对应测试放在 `keystone-framework-domain/src/test/java/app/keystone/domain/system/SystemExportApplicationServiceTest.java`。

## 发布维护

框架相关发布模块：

```text
keystone-common
keystone-infrastructure
keystone-framework-domain
keystone-framework-admin
keystone-framework-spring-boot-starter
```

发布配置在根 `build.gradle` 中维护：

```gradle
configure([
    project(':keystone-common'),
    project(':keystone-infrastructure'),
    project(':keystone-framework-domain'),
    project(':keystone-framework-admin'),
    project(':keystone-framework-spring-boot-starter')
]) {
    apply plugin: 'maven-publish'
}
```

本地发布命令：

```powershell
.\gradlew.bat :keystone-common:publishToMavenLocal :keystone-infrastructure:publishToMavenLocal :keystone-framework-domain:publishToMavenLocal :keystone-framework-admin:publishToMavenLocal :keystone-framework-spring-boot-starter:publishToMavenLocal
```

版本号来自根 `build.gradle`：

```gradle
allprojects {
    group = 'app.keystone'
    version = '3.6.1'
}
```

升级版本时要同步检查：

- 根项目 `version`
- 发布后的 POM
- 外部应用引用版本
- 文档中的版本示例

当前没有配置远程 Maven 仓库发布地址。接入内部仓库时，应在 `publishing.repositories` 增加内部仓库配置，并使用环境变量或 CI Secret 注入凭据。

## 迁移资源维护

迁移资源分层必须保持：

```text
keystone-infrastructure/src/main/resources/db/migrate/common
keystone-infrastructure/src/main/resources/db/migrate/mysql
```

规则：

- `keystone-infrastructure` 只放框架/系统迁移。
- 下游应用业务迁移不放入开源框架模块。
- 开源分支不维护独立测试数据库 SQL；数据库集成测试使用 Docker MySQL 和 Flyway 主迁移脚本。
- 不要把业务迁移放回 infrastructure 主资源，否则 starter 会重新携带业务表。
- 迁移文件名继续使用 `V<版本号>__<描述>.sql`。
- 已执行过的迁移不要改名、不要改版本号。
- 初始化数据使用 `INSERT IGNORE`。
- 新增列和索引用 `information_schema` 判断后再执行 DDL。

维护后必须检查发布物资源：

```powershell
.\gradlew.bat :keystone-infrastructure:jar :keystone-domain:jar
jar tf keystone-infrastructure\build\libs\keystone-infrastructure-3.6.1.jar | Select-String "db/migrate"
jar tf keystone-infrastructure\build\libs\keystone-infrastructure-3.6.1.jar | Select-String "business"
jar tf keystone-domain\build\libs\keystone-domain-3.6.1.jar | Select-String "db/migrate"
```

期望：

- infrastructure jar 能看到 `db/migrate/common` 和框架 `db/migrate/mysql`。
- infrastructure jar 看不到 `business`。
- domain jar 不携带主资源迁移。

## 测试门禁

框架 starter 相关改动至少运行：

```powershell
.\gradlew.bat :keystone-framework-spring-boot-starter:test :keystone-framework-domain:test :keystone-framework-admin:test
```

涉及应用扩展模块或迁移资源时运行：

```powershell
.\gradlew.bat :keystone-domain:test :keystone-admin:test :keystone-admin:compileJava
```

涉及数据库集成测试时，先启动 Docker MySQL 和 Redis：

```powershell
cd docker
docker compose up -d mysql redis
cd ..
.\gradlew.bat :keystone-domain:integrationTest :keystone-admin:dbIntegrationTest
```

集成测试任务会重建专用数据库：

```text
keystone_domain_integration_test
keystone_admin_integration_test
```

涉及发布配置时运行：

```powershell
.\gradlew.bat :keystone-common:publishToMavenLocal :keystone-infrastructure:publishToMavenLocal :keystone-framework-domain:publishToMavenLocal :keystone-framework-admin:publishToMavenLocal :keystone-framework-spring-boot-starter:publishToMavenLocal
```

当前 `GenerateMavenPom` 在 configuration cache 下可能输出 warning。只要任务结果是 `BUILD SUCCESSFUL`，发布可用；后续可以单独处理 configuration-cache 兼容性。

## 回归测试重点

必须保留或补充以下测试：

- framework admin auto-configuration imports 可加载。
- framework domain auto-configuration imports 可加载。
- infrastructure auto-configuration 扫描 common/infrastructure。
- starter 类路径可看到框架自动配置。
- starter 类路径看不到业务迁移和测试数据库脚本。
- Keystone 主应用数据库集成测试能通过 Docker MySQL 和 Flyway 主迁移脚本启动。

当前关键测试：

```text
keystone-framework-admin/src/test/java/app/keystone/framework/admin/autoconfigure/KeystoneFrameworkAdminAutoConfigurationTest.java
keystone-framework-domain/src/test/java/app/keystone/framework/domain/autoconfigure/KeystoneFrameworkDomainAutoConfigurationTest.java
keystone-framework-domain/src/test/java/app/keystone/domain/system/SystemExportApplicationServiceTest.java
keystone-framework-domain/src/test/java/app/keystone/domain/system/job/runtime/JobInvokeUtilTest.java
keystone-framework-domain/src/test/java/app/keystone/domain/system/job/runtime/JobSchedulerManagerTest.java
keystone-framework-spring-boot-starter/src/test/java/app/keystone/framework/starter/KeystoneFrameworkStarterDependencyTest.java
keystone-domain/src/test/java/app/keystone/integrationTest/DockerMySqlIntegrationTest.java
keystone-admin/src/test/java/app/keystone/admin/config/SwaggerEnabledIntegrationTest.java
keystone-admin/src/test/java/app/keystone/admin/config/SwaggerDisabledIntegrationTest.java
```

## 添加新框架能力的流程

1. 判断能力是否属于框架。
2. 领域逻辑放 `keystone-framework-domain`。
3. Web/API 层放 `keystone-framework-admin`。
4. 基础设施支撑放 `keystone-infrastructure`。
5. 系统表迁移放 `keystone-infrastructure/src/main/resources/db/migrate/mysql`。
6. 更新自动配置和测试。
7. 运行测试门禁和发布验证。

判断标准：

- 可被多个业务应用复用，且不依赖具体业务实体：框架能力。
- 只服务具体下游应用场景：业务能力。

## 下游应用添加业务能力的流程

1. 业务领域代码放在下游应用自己的领域模块。
2. 业务 controller 放在下游应用自己的 Web 模块。
3. 业务迁移放在下游应用自己的迁移路径。
4. 不修改 starter 依赖，除非该能力被明确提升为框架能力。

## 后续演进建议

优先级从高到低：

1. 增加远程 Maven 仓库发布配置和 CI 发布流程。
2. 将 `application-basic.yml` 拆成 starter 默认配置和 Keystone 应用配置。
3. 将历史包名迁移到明确框架命名空间，例如 `app.keystone.framework.*`。
4. 继续收敛 `keystone-infrastructure` 的依赖，减少 starter 传递依赖体积。
5. 为外部 demo 应用增加集成测试，验证只依赖 starter 即可启动框架系统能力。
