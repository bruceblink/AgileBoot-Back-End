# 定时任务开发指南

## 1. 适用场景

本文面向后续开发者，说明如何在 Keystone 中新增一个可被管理端配置的定时任务。

适合接入定时任务的场景：

| 场景 | 示例 |
| --- | --- |
| 数据清理 | 清理过期临时文件、清理历史会话 |
| 缓存刷新 | 刷新设备字典缓存、刷新统计缓存 |
| 状态同步 | 同步外部系统状态、拉取设备状态 |
| 异步汇总 | 汇总日报、生成统计快照 |
| 巡检检测 | 检查数据一致性、检查过期任务 |

不适合直接接入的场景：

| 场景 | 原因 |
| --- | --- |
| 强实时任务 | Cron 调度不是实时消息队列 |
| 长事务任务 | 容易阻塞调度线程和数据库连接 |
| 需要复杂运行时输入的任务 | 定时任务参数适合稳定配置，不适合作为每次执行动态输入 |
| 多步骤编排 | 当前不支持 DAG、补偿、依赖关系 |
| 必须全局单实例运行的任务 | 多节点部署时当前锁不是分布式锁 |

## 2. 开发流程总览

```text
1. 定义任务 Bean
2. 添加 @JobTask 注解
3. 保证方法无参或只接收一个参数对象、可重复执行、异常可观测
4. 编写单元测试或集成测试
5. 启动后在管理端选择调用目标
6. 配置 Cron、并发策略、状态
7. 点击任务编号查看运行日志
```

## 3. 编写任务 Bean

推荐把定时任务入口放在清晰的包路径中，例如：

```text
keystone-infrastructure/src/main/java/app/keystone/infrastructure/schedule
keystone-framework-domain/src/main/java/app/keystone/domain/system/job/task
<business-app-domain>/src/main/java/.../{module}/job
```

如果任务只是基础设施类维护动作，例如清理临时文件、刷新公共缓存，可以放在 `keystone-infrastructure`。

如果任务属于 Keystone 框架系统能力，例如清理 `sys_job_log`，放在 `keystone-framework-domain`。

如果任务明显属于某个业务领域，例如工单超时扫描、设备状态同步，应放在消费应用自己的领域模块下，不放回开源框架模块。当前开源仓库中的 `keystone-domain` 仅作为下游扩展模块占位。

## 4. 标准代码模板

```java
package app.keystone.domain.example.job;

import app.keystone.common.annotation.JobTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("exampleJobTask")
@RequiredArgsConstructor
public class ExampleJobTask {

    private final ExampleApplicationService exampleApplicationService;

    @JobTask(name = "示例任务", group = "示例模块", description = "演示如何接入数据库调度")
    public void runExampleJob() {
        log.info("example scheduled job started");
        exampleApplicationService.handleScheduledJob();
        log.info("example scheduled job finished");
    }
}
```

管理端可选择的调用目标为：

```text
exampleJobTask.runExampleJob()
```

## 5. `@JobTask` 使用规范

`@JobTask` 用于声明方法可以被数据库定时任务调用。

| 属性 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 建议填写 | 管理端显示名称 |
| `group` | 建议填写 | 管理端分组 |
| `description` | 建议填写 | 说明任务用途、影响范围 |

示例：

```java
@JobTask(
    name = "刷新设备字典缓存",
    group = "设备字典",
    description = "重新加载设备字典类型和字典项缓存"
)
public void refreshDeviceDictionaryCache() {
    deviceDictionaryApplicationService.refreshCache();
}
```

## 6. 方法签名要求

任务方法必须满足：

1. `public` 或可反射访问。
2. 无参数，或只接收一个参数对象。
3. 非静态方法。
4. 所在类是 Spring Bean。
5. 同一个 Bean 中不要定义同名可调度方法。

允许：

```java
@JobTask(name = "清理缓存", group = "系统")
public void cleanCache() {
}
```

不允许：

```java
@JobTask(name = "按天清理", group = "系统")
public void cleanByDay(int days) {
}
```

如果需要参数，请使用参数对象，并在管理端“任务参数”中填写 JSON：

```java
@JobTask(name = "按天清理", group = "系统")
public void cleanByDay(CleanByDayParams params) {
    int days = params.daysOrDefault();
}

public static class CleanByDayParams {
    private Integer days = 30;

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public int daysOrDefault() {
        return days == null || days < 1 ? 30 : days;
    }
}
```

```json
{
  "days": 60
}
```

## 7. Bean 命名规范

建议显式指定 Bean 名称，避免类名重构影响 `invokeTarget`：

```java
@Component("deviceDictionaryJobTask")
public class DeviceDictionaryJobTask {
}
```

命名建议：

| 类型 | 命名 |
| --- | --- |
| 系统任务 | `systemJobTask` |
| 缓存任务 | `{module}CacheJobTask` |
| 设备任务 | `deviceJobTask` |
| 工单任务 | `workOrderJobTask` |

方法名建议使用动词开头：

```text
refreshCache()
syncStatus()
cleanExpiredData()
scanOverdueWorkOrders()
```

## 8. 管理端配置步骤

1. 进入“系统管理 / 定时任务管理”。
2. 点击“添加任务”。
3. 填写任务名称和任务组。
4. 在“调用目标”中选择 `@JobTask` 暴露的方法。
5. 如果目标方法接收参数对象，在“任务参数”中填写 JSON，例如 `{"retentionDays":60}`。
6. 填写 Cron 表达式。
7. 选择是否允许并发。
8. 设置状态：
   - `正常`：保存后立即注册调度。
   - `暂停`：只保存定义，不注册调度。
9. 保存。
10. 可点击“执行”手动验证。
11. 点击任务编号查看运行日志。

## 9. Cron 配置参考

Keystone 使用 Spring Cron 表达式，格式为 6 位：

```text
秒 分 时 日 月 周
```

常用示例：

| 表达式 | 说明 |
| --- | --- |
| `0 0/5 * * * *` | 每 5 分钟 |
| `0 0/30 * * * *` | 每 30 分钟 |
| `0 0 0 * * *` | 每天 00:00 |
| `0 0 2 * * *` | 每天 02:00 |
| `0 0 1 * * MON` | 每周一 01:00 |
| `0 0 3 1 * *` | 每月 1 日 03:00 |

不建议使用过高频率，例如每秒执行一次。高频任务会增加：

1. 调度线程压力。
2. 数据库运行日志写入压力。
3. 业务资源竞争。

## 10. 并发策略选择

| 任务类型 | 建议 concurrent |
| --- | --- |
| 幂等、短耗时、允许重入 | `1` 允许并发 |
| 清理类任务 | `0` 禁止并发 |
| 同步外部系统 | `0` 禁止并发 |
| 生成报表/汇总 | `0` 禁止并发 |
| 缓存刷新 | 通常 `0` 禁止并发 |

禁止并发不是“排队执行”，而是“正在执行则跳过本次触发”。跳过会写入运行日志，状态为“跳过”。

## 11. 异常处理规范

任务方法内部不应吞掉关键异常。

推荐：

```java
@JobTask(name = "同步设备状态", group = "设备")
public void syncDeviceStatus() {
    deviceSyncService.syncAll();
}
```

如果需要捕获异常补充上下文，捕获后应重新抛出：

```java
@JobTask(name = "同步设备状态", group = "设备")
public void syncDeviceStatus() {
    try {
        deviceSyncService.syncAll();
    } catch (RuntimeException e) {
        log.error("sync device status failed", e);
        throw e;
    }
}
```

不推荐：

```java
@JobTask(name = "同步设备状态", group = "设备")
public void syncDeviceStatus() {
    try {
        deviceSyncService.syncAll();
    } catch (Exception e) {
        log.warn("ignored", e);
    }
}
```

吞掉异常会导致 `sys_job_log` 记录为成功，影响排查。

## 12. 事务边界建议

定时任务入口方法不建议直接承载过长事务。

建议把业务拆成：

1. 入口方法：记录开始和结束，调用应用服务。
2. 应用服务：按批次处理。
3. 单批事务：控制每次事务范围。

示例：

```java
@JobTask(name = "清理过期文件", group = "文件")
public void cleanExpiredFiles() {
    fileCleanupApplicationService.cleanExpiredFiles();
}
```

应用服务内部按批次分页处理，避免一次事务覆盖大量数据。

## 13. 幂等性要求

所有定时任务都应尽量幂等。至少需要考虑：

1. 同一任务被手动执行和自动调度同时触发。
2. 应用重启后任务再次执行。
3. 外部接口超时后重试。
4. 上一次执行到一半失败，下次继续执行。

常见做法：

| 场景 | 做法 |
| --- | --- |
| 清理数据 | 按状态和时间条件删除，重复执行无影响 |
| 生成统计 | 使用日期作为唯一键，存在则更新 |
| 同步外部状态 | 使用外部 ID 做幂等键 |
| 发送通知 | 保存发送记录，避免重复发送 |

## 14. 测试建议

至少覆盖：

1. 任务方法本身的业务测试。
2. `@JobTask` 方法出现在 `/system/jobs/invoke-targets`。
3. 手动执行成功时写入成功运行日志。
4. 手动执行失败时写入失败运行日志。
5. 禁止并发任务被重复触发时写入跳过日志。

现有参考测试：

```text
keystone-framework-domain/src/test/java/app/keystone/domain/system/job/runtime/JobInvokeUtilTest.java
keystone-framework-domain/src/test/java/app/keystone/domain/system/job/runtime/JobSchedulerManagerTest.java
keystone-framework-domain/src/test/java/app/keystone/domain/system/job/task/SysJobLogCleanupTaskTest.java
```

## 15. 排查指南

### 15.1 调用目标列表里看不到任务

检查：

1. 类是否有 `@Component`、`@Service` 等 Spring Bean 注解。
2. Bean 是否在当前应用组件扫描范围内，或由 starter 自动配置加载。
3. 方法是否无参，或只接收一个参数对象。
4. 方法是否标注 `@JobTask`。
5. 应用是否重启或重新加载。

### 15.2 保存任务提示 Bean 不存在

检查 `invokeTarget` 的 Bean 名称是否正确。

例如：

```java
@Component("demoJobTask")
public class DemoJobTask {
}
```

调用目标应为：

```text
demoJobTask.printHeartbeat()
```

### 15.3 保存任务提示方法不存在

检查：

1. 方法名是否拼写正确。
2. 方法是否无参，或只接收一个参数对象。
3. 同一个 Bean 中是否存在同名可调度方法。
4. 方法是否在目标 Bean 类上。
5. 如果使用代理，方法是否能被代理对象访问。

### 15.4 任务没有自动执行

检查：

1. `sys_job.status` 是否为 `1`。
2. Cron 表达式是否符合预期。
3. 应用启动日志是否有任务加载异常。
4. 任务是否被暂停后未恢复。
5. 多环境中是否连接到了正确数据库。

### 15.5 运行日志显示跳过

说明任务配置为禁止并发，并且上一次执行还没有结束。

处理方式：

1. 检查任务是否耗时过长。
2. 调整 Cron 间隔。
3. 优化任务执行速度。
4. 确认是否可以允许并发。

### 15.6 运行日志显示失败

查看：

1. `job_message` 的失败摘要。
2. `exception_info` 的异常栈摘要。
3. 应用日志中同一时间点的完整日志。
4. 相关业务表或外部服务状态。

## 16. 示例任务

当前项目提供了 `DemoJobTask` 作为测试样例：

| 展示名称 | 调用目标 | 用途 |
| --- | --- | --- |
| 打印心跳日志 | `demoJobTask.printHeartbeat()` | 验证任务能触发 |
| 模拟缓存刷新 | `demoJobTask.refreshDemoCache()` | 验证业务型无参任务 |
| 模拟耗时任务 | `demoJobTask.simulateLongRunningJob()` | 验证禁止并发配置 |
| 清理定时任务运行日志 | `sysJobLogCleanupTask.cleanExpiredJobLogs()` | 验证参数化任务，参数示例 `{"retentionDays":60}` |

这些任务无业务副作用，可用于本地验证定时任务管理页面和运行日志功能。

## 17. 上线检查清单

新增任务上线前检查：

1. Bean 名称固定且符合命名规范。
2. 方法已添加 `@JobTask`。
3. 方法无参，或只接收一个参数对象。
4. 任务逻辑幂等。
5. 异常不会被静默吞掉。
6. 处理大数据量时有批次控制。
7. 明确并发策略。
8. 明确 Cron 频率。
9. 已通过手动执行验证。
10. 已查看 `sys_job_log` 运行结果。
11. 如有外部接口调用，已考虑超时和重试边界。
12. 如有敏感信息，日志中不会输出明文。

