# 定时作业功能设计

## 1. 背景与目标

Keystone 定时作业用于把后台周期性任务从代码中的固定 `@Scheduled` 配置，升级为可在管理端配置、启停、立即执行、查看运行历史的能力。

当前设计解决三个问题：

1. 运维人员不需要知道所有 Spring Bean 名称，也不需要手写不可发现的调用目标。
2. 任务运行结果需要落库，便于排查执行失败、并发跳过和耗时异常。
3. 定时任务管理操作需要进入系统操作日志，和其它后台管理动作保持一致。

## 2. 设计范围

### 2.1 已实现能力

| 能力 | 说明 |
| --- | --- |
| 任务定义管理 | 新增、修改、删除、查询 `sys_job` |
| 任务启停 | 修改任务状态后动态注册或取消调度 |
| 立即运行 | 通过管理端手动触发一次任务 |
| 调用目标发现 | 扫描 `@JobTask` 和 `@Scheduled` 方法，返回候选列表 |
| 任务参数 | `sys_job.job_params` 保存 JSON 参数，支持目标方法接收一个参数对象 |
| 运行历史 | 每次自动调度、手动执行、失败、并发跳过写入 `sys_job_log` |
| 操作审计 | 新增、修改、启停、立即运行、删除进入 `sys_operation_log` |
| 前端辅助 | 调用目标下拉选择，点击任务编号查看运行日志 |

### 2.2 非目标

| 非目标 | 原因 |
| --- | --- |
| 分布式任务锁 | 当前运行态注册表是单 JVM 内存结构，多节点部署时需要单独设计分布式锁 |
| Cron 在线解析器 | 只做后端 Cron 语法校验，不在后端提供复杂表达式解释 |
| 调度历史清理策略 | 当前只记录运行历史，后续可按保留天数或最大条数增加清理任务 |
| 任务依赖编排 | 不支持 DAG、前置任务、失败重试编排 |

## 3. 关键概念

| 概念 | 说明 |
| --- | --- |
| 任务定义 | `sys_job` 中的一条记录，描述任务名称、调用目标、任务参数、Cron、并发策略、状态 |
| 调用目标 | Spring Bean 方法，格式为 `springBean.method()` |
| 任务参数 | `job_params` 中保存的 JSON，目标方法有一个参数对象时由后端反序列化传入 |
| 候选目标 | 后端扫描得到的可选调用目标，前端用于下拉选择 |
| 运行日志 | `sys_job_log` 中的一条执行记录 |
| 操作日志 | `sys_operation_log` 中的一条管理操作审计记录 |
| 自动调度 | 根据 Cron 触发任务执行 |
| 手动执行 | 用户点击“执行”触发一次任务 |
| 并发跳过 | 禁止并发的任务仍在执行，新触发被跳过并写入运行日志 |

## 4. 模块结构

```text
keystone-admin
  app.keystone.admin.controller.system.SysJobController

keystone-common
  app.keystone.common.annotation.JobTask
  app.keystone.common.enums.common.JobLogStatusEnum
  app.keystone.common.enums.common.JobTriggerTypeEnum

keystone-domain
  app.keystone.domain.system.job.JobApplicationService
  app.keystone.domain.system.job.db.SysJobEntity
  app.keystone.domain.system.job.db.SysJobLogEntity
  app.keystone.domain.system.job.runtime.JobInvokeUtil
  app.keystone.domain.system.job.runtime.JobSchedulerManager
  app.keystone.domain.system.job.runtime.JobStartupRunner

keystone-infrastructure
  db/migrate/mysql/V3_5_46__add_dict_and_job_management.sql
  db/migrate/mysql/V3_6_10__add_sys_job_log.sql
  app.keystone.infrastructure.schedule.DemoJobTask
```

## 5. 数据模型

### 5.1 任务定义表 `sys_job`

`sys_job` 保存可调度任务的定义。

| 字段 | 说明 |
| --- | --- |
| `job_id` | 任务 ID |
| `job_name` | 任务名称 |
| `job_group` | 任务组名 |
| `invoke_target` | 调用目标，格式 `springBean.method()` |
| `job_params` | 任务参数 JSON，可为空；目标方法接收一个参数对象时由后端反序列化传入 |
| `cron_expression` | Spring Cron 表达式 |
| `concurrent` | 是否允许并发执行，`1` 允许，`0` 禁止 |
| `status` | 任务状态，`1` 正常，`0` 暂停 |
| `remark` | 备注 |
| `creator_id` / `create_time` | 创建审计字段 |
| `updater_id` / `update_time` | 修改审计字段 |

### 5.2 任务运行日志表 `sys_job_log`

`sys_job_log` 保存每次任务执行的结果。任务删除后运行日志不级联删除，用于保留历史审计信息。

| 字段 | 说明 |
| --- | --- |
| `job_log_id` | 运行日志 ID |
| `job_id` | 任务 ID，任务删除后可为空语义上仍保留快照 |
| `job_name` | 执行时任务名称快照 |
| `job_group` | 执行时任务组快照 |
| `invoke_target` | 执行时调用目标快照 |
| `job_params` | 执行时任务参数 JSON 快照 |
| `cron_expression` | 执行时 Cron 快照 |
| `trigger_type` | 触发类型，`1` 自动调度，`2` 手动执行 |
| `status` | 执行状态，`1` 成功，`0` 失败，`2` 跳过 |
| `job_message` | 简短执行信息 |
| `exception_info` | 失败异常栈摘要 |
| `start_time` | 开始时间 |
| `end_time` | 结束时间 |
| `duration_ms` | 耗时毫秒 |
| `create_time` | 日志创建时间 |

### 5.3 系统操作日志 `sys_operation_log`

定时任务管理接口通过 `@AccessLog` 写入现有操作日志。

| 操作 | 接口 | businessType |
| --- | --- | --- |
| 新增任务 | `POST /system/jobs` | `ADD` |
| 修改任务 | `PUT /system/jobs/{jobId}` | `MODIFY` |
| 修改状态 | `PUT /system/jobs/{jobId}/status` | `MODIFY` |
| 立即运行 | `POST /system/jobs/{jobId}/run` | `OTHER` |
| 删除任务 | `DELETE /system/jobs` | `DELETE` |

运行日志查询是只读查询，不写操作日志，避免普通列表查询产生大量审计噪音。

## 6. API 设计

### 6.1 任务定义

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/system/jobs` | 分页查询任务 | `system:job:list` |
| `GET` | `/system/jobs/{jobId}` | 查询任务详情 | `system:job:query` |
| `POST` | `/system/jobs` | 新增任务 | `system:job:add` |
| `PUT` | `/system/jobs/{jobId}` | 修改任务 | `system:job:edit` |
| `PUT` | `/system/jobs/{jobId}/status` | 修改任务状态 | `system:job:changeStatus` |
| `POST` | `/system/jobs/{jobId}/run` | 立即运行 | `system:job:run` |
| `DELETE` | `/system/jobs` | 删除任务 | `system:job:remove` |

### 6.2 辅助接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| `GET` | `/system/jobs/invoke-targets` | 查询可调用目标列表 | `system:job:list` |
| `GET` | `/system/jobs/logs` | 分页查询运行日志 | `system:job:list` |

## 7. 调用目标发现

`JobInvokeUtil` 扫描 Spring 容器中的 Bean 方法，满足以下条件的方法会出现在候选列表：

1. 方法无参数，或只有一个参数对象。
2. 方法不是静态方法。
3. 方法标注了 `@JobTask`，或标注了 `@Scheduled` / `@Schedules`。
4. 同一个 Bean 中没有同名可调度方法。

推荐使用 `@JobTask` 作为数据库调度任务入口：

```java
@Slf4j
@Component("demoJobTask")
public class DemoJobTask {

    @JobTask(name = "打印心跳日志", group = "示例任务", description = "用于验证定时任务是否能按计划触发")
    public void printHeartbeat() {
        log.info("demo scheduled job heartbeat");
    }
}
```

候选接口返回的关键字段：

| 字段 | 说明 |
| --- | --- |
| `invokeTarget` | 可直接保存到 `sys_job.invoke_target` 的调用目标 |
| `beanName` | Spring Bean 名称 |
| `methodName` | 方法名 |
| `name` | 展示名称 |
| `group` | 展示分组 |
| `description` | 说明 |

## 8. 执行链路

### 8.1 应用启动

```text
应用启动
  -> JobStartupRunner 查询 status=1 的任务
  -> JobSchedulerManager.schedule(job)
  -> TaskScheduler 注册 CronTrigger
```

如果数据库表不存在，启动加载会容错并记录日志，避免首次迁移前阻断应用启动。

### 8.2 新增或修改任务

```text
SysJobController
  -> JobApplicationService.validateJob
      -> 校验状态、并发值、Cron 表达式、调用目标、任务参数 JSON
  -> 保存 sys_job
  -> JobSchedulerManager.schedule(job)
      -> 先 cancel 旧任务
      -> 如果 status=1 注册新调度
```

### 8.3 修改状态

```text
启用任务
  -> 更新 sys_job.status=1
  -> schedule(job)

暂停任务
  -> 更新 sys_job.status=0
  -> schedule(job)
      -> cancel(jobId)
      -> 不注册新调度
```

### 8.4 自动执行

```text
TaskScheduler 触发
  -> JobSchedulerManager.runSafely
  -> runWithLog(job, AUTO)
  -> invoke(job)
      -> 无参方法直接调用
      -> 单参数方法将 sys_job.job_params 反序列化为参数对象后调用
  -> 写 sys_job_log，包含 job_params 执行快照
```

自动执行失败时：

1. `sys_job_log.status=0`。
2. `exception_info` 保存异常栈摘要。
3. `JobSchedulerManager` 记录 error 日志。
4. 异常不向调度线程外继续扩散，避免影响后续调度。

### 8.5 手动执行

```text
POST /system/jobs/{jobId}/run
  -> JobApplicationService.runJobOnce
  -> validateJob
  -> JobSchedulerManager.runOnce
  -> runWithLog(job, MANUAL)
  -> 写 sys_job_log
```

手动执行失败时：

1. 写失败运行日志。
2. 异常继续抛给接口层。
3. `@AccessLog` 记录本次接口操作失败。

## 9. 并发控制

`sys_job.concurrent` 控制同一任务是否允许并发执行：

| 值 | 说明 |
| --- | --- |
| `1` | 允许并发，新触发直接执行 |
| `0` | 禁止并发，同一 `jobId` 使用 JVM 内 `ReentrantLock` 控制 |

禁止并发时，如果上一次任务尚未结束，本次触发不会阻塞等待，而是直接跳过，并写入：

```text
sys_job_log.status = 2
sys_job_log.job_message = 任务正在执行，本次触发已跳过
```

当前并发锁是单 JVM 内存锁。多实例部署时，不同实例之间仍可能同时执行同一个任务。多实例部署前应补充数据库锁、Redis 锁或使用具备集群协调能力的调度框架。

## 10. Cron 规则

后端使用 Spring `CronExpression.parse` 校验 Cron 表达式。

示例：

| 表达式 | 说明 |
| --- | --- |
| `0 0/5 * * * *` | 每 5 分钟执行一次 |
| `0 0 2 * * *` | 每天 02:00 执行 |
| `0 0 1 * * MON` | 每周一 01:00 执行 |

注意：

1. Spring Cron 为 6 位格式，第一位是秒。
2. 管理端保存前会校验表达式，不合法会返回业务错误。
3. 高频任务会增加数据库运行日志写入压力，应谨慎配置。

## 11. 安全与约束

1. 任务方法必须是无参方法，或只接收一个参数对象。
2. 参数必须保存为 JSON，后端会在保存任务前校验是否能反序列化为目标参数类型。
3. 同一个 Bean 中不要定义同名可调度方法，否则 `springBean.method()` 无法唯一定位。
4. 任务方法应由 Spring 容器管理，调用目标 Bean 必须存在。
5. 推荐只把明确允许被管理端调度的方法标注为 `@JobTask`。
6. 运行方法内部应自行处理业务幂等，尤其是通知、同步、清理类任务。
7. 不建议把 HTTP 请求参数、用户输入或动态脚本直接拼接成调用目标。
8. 任务日志保存异常栈摘要和参数快照，不应主动写入敏感数据。

## 12. 已知限制与后续演进

| 方向 | 建议 |
| --- | --- |
| 分布式调度 | 增加 Redis/DB 分布式锁，或迁移到 Quartz/ShedLock |
| 历史清理 | 增加任务日志保留天数配置和清理任务 |
| 失败重试 | 在任务定义中增加重试次数、重试间隔 |
| 告警通知 | 对连续失败、耗时过长、跳过过多增加告警 |
| 参数表单化 | 基于参数对象生成前端动态表单，替代手写 JSON |
| 权限细化 | 运行日志可拆出 `system:job:log` 权限 |

