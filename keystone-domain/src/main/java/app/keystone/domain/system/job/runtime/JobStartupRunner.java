package app.keystone.domain.system.job.runtime;

import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.domain.system.job.db.SysJobEntity;
import app.keystone.domain.system.job.db.SysJobService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads enabled scheduled jobs after the application starts.
 * @author likanug
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobStartupRunner implements ApplicationRunner {

    private final SysJobService jobService;
    private final JobSchedulerManager jobSchedulerManager;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<SysJobEntity> enabledJobs = jobService.lambdaQuery()
                .eq(SysJobEntity::getStatus, JobStatusEnum.ENABLE.getValue())
                .list();
            enabledJobs.forEach(jobSchedulerManager::schedule);
        } catch (RuntimeException e) {
            if (isMissingJobTable(e)) {
                log.warn("skip scheduled job startup loading because sys_job table is not initialized: {}",
                    e.getMessage());
                return;
            }
            throw e;
        }
    }

    private boolean isMissingJobTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("sys_job")
                && (message.toLowerCase().contains("not found")
                    || message.toLowerCase().contains("doesn't exist")
                    || message.toLowerCase().contains("does not exist"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
