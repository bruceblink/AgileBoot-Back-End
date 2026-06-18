# 框架代码与业务代码隔离方案

## 可行性评估

第一条低风险边界是 admin Web 层。当前 `keystone-admin` 模块同时包含框架接口和业务接口，但 `controller/system`、`controller/common`、`customize` 这些包主要依赖系统领域服务和公共基础设施，可以在不修改请求路径和 Java 包名的前提下拆到独立模块。

完整拆成 Spring Boot starter 是可行的，当前已经进入可用阶段。框架代码已经隔离到独立 Gradle 模块，可以在仓库内独立开发；starter 聚合模块提供统一的框架入口，消费应用不需要逐个装配框架子模块。

相关文档：

- [Keystone Framework Starter 使用说明](framework-starter-usage.md)
- [Keystone Framework Starter 维护文档](framework-starter-maintenance.md)

## 已实现

- 新增 `keystone-framework-admin`。
- 将 admin 框架控制器从 `keystone-admin/src/main/java/app/keystone/admin/controller/common` 和 `controller/system` 移出。
- 将 admin 框架支撑代码从 `keystone-admin/src/main/java/app/keystone/admin/customize` 移出。
- 移动 `controller/system` 和 `customize` 对应的单元测试。
- 新增 `keystone-framework-domain`。
- 将 `domain/common` 和 `domain/system` 移入 `keystone-framework-domain`。
- 为 `keystone-framework-admin` 和 `keystone-framework-domain` 增加 Spring Boot 自动配置元数据。
- 增加框架基础设施自动配置，starter 依赖可自动扫描 `common` 配置和 `infrastructure` 组件。
- 新增 `keystone-framework-spring-boot-starter` 作为框架聚合依赖。
- 为 common、infrastructure、框架 domain、框架 admin 和 starter 模块增加 Maven publication 元数据，支持发布为 starter 制品。
- `keystone-domain` 保留为下游应用扩展模块，并由 `keystone-domain` 依赖 `keystone-framework-domain`。
- 下游应用的业务迁移由消费方自行提供，框架 starter 传递依赖不携带业务表迁移。
- 将 H2 集成测试 schema/data 移出 infrastructure 主资源，改为 admin/domain 测试资源。
- 将 `keystone-admin` 调整为依赖 `keystone-framework-spring-boot-starter` 和扩展模块 `keystone-domain`。
- `keystone-admin` 启动类移除全仓 `@ComponentScan("app.keystone.*")`，框架组件由 starter 自动配置装配。
- 删除 `keystone-admin` 中重复的系统监控 controller，`/monitor/**` 由 `keystone-framework-admin` 提供。
- 将定时任务运行日志清理任务和定时任务运行时相关测试迁入 `keystone-framework-domain`。
- 系统导出接口改为调用无分页 `export...` 方法，避免只导出当前分页。
- `keystone-domain/src/main/java` 当前保持为空，作为下游扩展模块边界。
- 移除 `domain/common/cache` 对具体业务实体的直接依赖，改为通过通用缓存名称查找。
- 暂时保持 Java 包名不变，确保 Spring 组件扫描、路由映射和现有 import 稳定。
- 保留 `keystone-admin` 作为可执行应用入口。

## 后续阶段

1. 在路由和 import 稳定后，将框架包名从 `app.keystone.admin.*` / `app.keystone.domain.*` 迁移到明确的框架命名空间。
2. 继续梳理 `keystone-infrastructure` 中的配置资源，按 starter 默认配置和 Keystone 应用配置拆分。
