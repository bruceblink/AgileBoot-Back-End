package app.keystone.domain.system.job.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.keystone.common.enums.common.JobLogStatusEnum;
import app.keystone.common.enums.common.JobTriggerTypeEnum;
import app.keystone.domain.system.job.db.SysJobEntity;
import app.keystone.domain.system.job.db.SysJobLogEntity;
import app.keystone.domain.system.job.db.SysJobLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

class JobSchedulerManagerTest {

    private TaskScheduler taskScheduler;
    private JobInvokeUtil jobInvokeUtil;
    private SysJobLogService jobLogService;
    private JobSchedulerManager manager;

    @BeforeEach
    void setUp() {
        taskScheduler = mock(TaskScheduler.class);
        jobInvokeUtil = mock(JobInvokeUtil.class);
        jobLogService = mock(SysJobLogService.class);
        manager = new JobSchedulerManager(taskScheduler, jobInvokeUtil, jobLogService);
    }

    @Test
    void runOnce_shouldRecordSuccessLog() {
        SysJobEntity job = job();

        manager.runOnce(job);

        SysJobLogEntity log = captureSavedLog();
        assertEquals(job.getJobId(), log.getJobId());
        assertEquals(job.getJobName(), log.getJobName());
        assertEquals(job.getJobGroup(), log.getJobGroup());
        assertEquals(job.getInvokeTarget(), log.getInvokeTarget());
        assertEquals(job.getJobParams(), log.getJobParams());
        assertEquals(JobTriggerTypeEnum.MANUAL.getValue(), log.getTriggerType());
        assertEquals(JobLogStatusEnum.SUCCESS.getValue(), log.getStatus());
        assertEquals("任务执行成功", log.getJobMessage());
        assertNotNull(log.getStartTime());
        assertNotNull(log.getEndTime());
        assertNotNull(log.getDurationMs());
        verify(jobInvokeUtil).invoke(job.getInvokeTarget(), job.getJobParams());
    }

    @Test
    void runOnce_shouldRecordFailureLogAndRethrow() {
        SysJobEntity job = job();
        doThrow(new IllegalStateException("boom")).when(jobInvokeUtil).invoke(job.getInvokeTarget(), job.getJobParams());

        assertThrows(IllegalStateException.class, () -> manager.runOnce(job));

        SysJobLogEntity log = captureSavedLog();
        assertEquals(JobTriggerTypeEnum.MANUAL.getValue(), log.getTriggerType());
        assertEquals(JobLogStatusEnum.FAIL.getValue(), log.getStatus());
        assertEquals("任务执行失败：boom", log.getJobMessage());
        assertNotNull(log.getExceptionInfo());
        assertNotNull(log.getEndTime());
        assertNotNull(log.getDurationMs());
    }

    private SysJobLogEntity captureSavedLog() {
        ArgumentCaptor<SysJobLogEntity> captor = ArgumentCaptor.forClass(SysJobLogEntity.class);
        verify(jobLogService).save(captor.capture());
        return captor.getValue();
    }

    private SysJobEntity job() {
        SysJobEntity job = new SysJobEntity();
        job.setJobId(10L);
        job.setJobName("demo");
        job.setJobGroup("DEFAULT");
        job.setInvokeTarget("demoJobTask.printHeartbeat()");
        job.setJobParams("{\"retentionDays\":60}");
        job.setCronExpression("0 * * * * *");
        job.setConcurrent(1);
        return job;
    }
}
