package app.keystone.domain.system.job.task;

import app.keystone.common.annotation.JobTask;
import app.keystone.domain.system.job.db.SysJobLogEntity;
import app.keystone.domain.system.job.db.SysJobLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Scheduled maintenance tasks for job execution logs.
 *
 * @author likanug
 */
@Slf4j
@Component("sysJobLogCleanupTask")
public class SysJobLogCleanupTask {

    private static final int JOB_LOG_RETENTION_DAYS = 30;

    private final SysJobLogService jobLogService;

    public SysJobLogCleanupTask(SysJobLogService jobLogService) {
        this.jobLogService = jobLogService;
    }

    @JobTask(name = "清理定时任务运行日志", group = "系统维护", description = "清理 sys_job_log 表中 30 天之前的历史数据")
    public void cleanExpiredJobLogs() {
        Date cutoffTime = Date.from(Instant.now().minus(JOB_LOG_RETENTION_DAYS, ChronoUnit.DAYS));
        QueryWrapper<SysJobLogEntity> wrapper = new QueryWrapper<SysJobLogEntity>()
            .lt("create_time", cutoffTime);
        long expiredCount = jobLogService.count(wrapper);
        if (expiredCount == 0) {
            log.info("no expired scheduled job logs to clean, cutoffTime={}", cutoffTime);
            return;
        }
        jobLogService.remove(wrapper);
        log.info("cleaned expired scheduled job logs, cutoffTime={}, count={}", cutoffTime, expiredCount);
    }
}
