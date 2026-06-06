package app.keystone.domain.system.job.runtime;

import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.domain.system.job.db.SysJobEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
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

    private final TaskScheduler taskScheduler;
    private final JobInvokeUtil jobInvokeUtil;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> jobLocks = new ConcurrentHashMap<>();

    public JobSchedulerManager(TaskScheduler taskScheduler, JobInvokeUtil jobInvokeUtil) {
        this.taskScheduler = taskScheduler;
        this.jobInvokeUtil = jobInvokeUtil;
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
        invoke(job);
    }

    public void clear() {
        scheduledTasks.keySet().forEach(this::cancel);
    }

    private void runSafely(SysJobEntity job) {
        try {
            invoke(job);
        } catch (RuntimeException e) {
            log.error("scheduled job execution failed, jobId={}, jobName={}", job.getJobId(), job.getJobName(), e);
        }
    }

    private void invoke(SysJobEntity job) {
        if (YesOrNoEnum.NO.getValue().equals(job.getConcurrent())) {
            ReentrantLock lock = jobLocks.computeIfAbsent(job.getJobId(), ignored -> new ReentrantLock());
            if (!lock.tryLock()) {
                log.warn("skip concurrent scheduled job, jobId={}, jobName={}", job.getJobId(), job.getJobName());
                return;
            }
            try {
                jobInvokeUtil.invoke(job.getInvokeTarget());
            } finally {
                lock.unlock();
            }
            return;
        }

        jobInvokeUtil.invoke(job.getInvokeTarget());
    }
}
