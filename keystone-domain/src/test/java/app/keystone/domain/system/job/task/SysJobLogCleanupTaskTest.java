package app.keystone.domain.system.job.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.domain.system.job.db.SysJobLogEntity;
import app.keystone.domain.system.job.db.SysJobLogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SysJobLogCleanupTaskTest {

    @Test
    void cleanExpiredJobLogs_shouldRemoveLogsCreatedBeforeThirtyDaysAgo() {
        SysJobLogService jobLogService = mock(SysJobLogService.class);
        when(jobLogService.count(any())).thenReturn(2L);
        SysJobLogCleanupTask task = new SysJobLogCleanupTask(jobLogService);
        Instant earliestCutoff = Instant.now().minus(30, ChronoUnit.DAYS).minus(2, ChronoUnit.SECONDS);

        task.cleanExpiredJobLogs();

        Instant latestCutoff = Instant.now().minus(30, ChronoUnit.DAYS).plus(2, ChronoUnit.SECONDS);
        QueryWrapper<SysJobLogEntity> wrapper = captureCountWrapper(jobLogService);
        String sqlSegment = wrapper.getSqlSegment();
        Date cutoffTime = (Date) wrapper.getParamNameValuePairs().values().iterator().next();
        assertFalse(cutoffTime.toInstant().isBefore(earliestCutoff));
        assertFalse(cutoffTime.toInstant().isAfter(latestCutoff));
        assertTrue(sqlSegment.contains("create_time <"));
        verify(jobLogService).remove(any());
    }

    @Test
    void cleanExpiredJobLogs_shouldSkipRemoveWhenNoExpiredLogsExist() {
        SysJobLogService jobLogService = mock(SysJobLogService.class);
        when(jobLogService.count(any())).thenReturn(0L);
        SysJobLogCleanupTask task = new SysJobLogCleanupTask(jobLogService);

        task.cleanExpiredJobLogs();

        verify(jobLogService, never()).remove(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private QueryWrapper<SysJobLogEntity> captureCountWrapper(SysJobLogService jobLogService) {
        ArgumentCaptor<QueryWrapper> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(jobLogService).count(captor.capture());
        return captor.getValue();
    }
}
