package app.keystone.domain.system.job.runtime;

import app.keystone.common.enums.common.JobLogStatusEnum;
import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.common.enums.common.JobTriggerTypeEnum;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.domain.system.job.db.SysJobEntity;
import app.keystone.domain.system.job.db.SysJobLogEntity;
import app.keystone.domain.system.job.db.SysJobLogService;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Runtime registry for database-backed scheduled jobs.
 * @author likanug
 */
@Slf4j
@Component
public class JobSchedulerManager {

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_EXCEPTION_LENGTH = 2000;

    private final TaskScheduler taskScheduler;
    private final JobInvokeUtil jobInvokeUtil;
    private final SysJobLogService jobLogService;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> jobLocks = new ConcurrentHashMap<>();

    public JobSchedulerManager(TaskScheduler taskScheduler, JobInvokeUtil jobInvokeUtil,
        SysJobLogService jobLogService) {
        this.taskScheduler = taskScheduler;
        this.jobInvokeUtil = jobInvokeUtil;
        this.jobLogService = jobLogService;
    }

    public void schedule(SysJobEntity job) {
        cancel(job.getJobId());
        if (!JobStatusEnum.ENABLE.getValue().equals(job.getStatus())) {
            return;
        }
        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> runSafely(job),
            new CronTrigger(job.getCronExpression())
        );
        if (future != null) {
            scheduledTasks.put(job.getJobId(), future);
        }
    }

    public void cancel(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void runOnce(SysJobEntity job) {
        runWithLog(job, JobTriggerTypeEnum.MANUAL);
    }

    public void clear() {
        scheduledTasks.keySet().forEach(this::cancel);
    }

    private void runSafely(SysJobEntity job) {
        try {
            runWithLog(job, JobTriggerTypeEnum.AUTO);
        } catch (RuntimeException e) {
            log.error("scheduled job execution failed, jobId={}, jobName={}", job.getJobId(), job.getJobName(), e);
        }
    }

    private void runWithLog(SysJobEntity job, JobTriggerTypeEnum triggerType) {
        Date startTime = new Date();
        long startNanos = System.nanoTime();
        SysJobLogEntity jobLog = createJobLog(job, triggerType, startTime);

        try {
            ExecutionResult result = invoke(job);
            if (result == ExecutionResult.SKIPPED) {
                jobLog.setStatus(JobLogStatusEnum.SKIPPED.getValue());
                jobLog.setJobMessage("任务正在执行，本次触发已跳过");
                return;
            }
            jobLog.setStatus(JobLogStatusEnum.SUCCESS.getValue());
            jobLog.setJobMessage("任务执行成功");
        } catch (RuntimeException e) {
            jobLog.setStatus(JobLogStatusEnum.FAIL.getValue());
            jobLog.setJobMessage(StringUtils.substring("任务执行失败：" + e.getMessage(), 0, MAX_MESSAGE_LENGTH));
            jobLog.setExceptionInfo(StringUtils.substring(ExceptionUtils.getStackTrace(e), 0, MAX_EXCEPTION_LENGTH));
            throw e;
        } finally {
            Date endTime = new Date();
            jobLog.setEndTime(endTime);
            jobLog.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
            saveJobLogSafely(jobLog);
        }
    }

    private SysJobLogEntity createJobLog(SysJobEntity job, JobTriggerTypeEnum triggerType, Date startTime) {
        SysJobLogEntity jobLog = new SysJobLogEntity();
        jobLog.setJobId(job.getJobId());
        jobLog.setJobName(job.getJobName());
        jobLog.setJobGroup(job.getJobGroup());
        jobLog.setInvokeTarget(job.getInvokeTarget());
        jobLog.setJobParams(job.getJobParams());
        jobLog.setCronExpression(job.getCronExpression());
        jobLog.setTriggerType(triggerType.getValue());
        jobLog.setStartTime(startTime);
        jobLog.setCreateTime(startTime);
        return jobLog;
    }

    private void saveJobLogSafely(SysJobLogEntity jobLog) {
        try {
            jobLogService.save(jobLog);
        } catch (RuntimeException e) {
            log.error("scheduled job log save failed, jobId={}, jobName={}", jobLog.getJobId(), jobLog.getJobName(), e);
        }
    }

    private ExecutionResult invoke(SysJobEntity job) {
        if (YesOrNoEnum.NO.getValue().equals(job.getConcurrent())) {
            ReentrantLock lock = jobLocks.computeIfAbsent(job.getJobId(), ignored -> new ReentrantLock());
            if (!lock.tryLock()) {
                log.warn("skip concurrent scheduled job, jobId={}, jobName={}", job.getJobId(), job.getJobName());
                return ExecutionResult.SKIPPED;
            }
            try {
                jobInvokeUtil.invoke(job.getInvokeTarget(), job.getJobParams());
            } finally {
                lock.unlock();
            }
            return ExecutionResult.SUCCESS;
        }

        jobInvokeUtil.invoke(job.getInvokeTarget(), job.getJobParams());
        return ExecutionResult.SUCCESS;
    }

    private enum ExecutionResult {
        SUCCESS,
        SKIPPED
    }
}
