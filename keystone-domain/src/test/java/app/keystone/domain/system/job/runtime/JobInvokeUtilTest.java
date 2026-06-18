package app.keystone.domain.system.job.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.keystone.common.annotation.JobTask;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.job.dto.JobInvokeTargetDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

class JobInvokeUtilTest {

    @Test
    void getAvailableInvokeTargets_shouldListAnnotatedAndScheduledMethodsWithZeroOrOneParam() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);

            List<JobInvokeTargetDTO> targets = util.getAvailableInvokeTargets();

            assertTrue(targets.stream().anyMatch(target -> target.getInvokeTarget().equals("sampleJob.clean()")
                && target.getName().equals("Clean data")
                && target.getGroup().equals("maintenance")
                && target.getDescription().equals("Remove expired data")));
            assertTrue(targets.stream().anyMatch(target -> target.getInvokeTarget().equals("sampleJob.report()")
                && target.getName().equals("report")
                && target.getGroup().equals("scheduled")));
            assertTrue(targets.stream().anyMatch(target -> target.getInvokeTarget().equals("sampleJob.withParams()")));
            assertFalse(targets.stream().anyMatch(target -> target.getInvokeTarget().equals("sampleJob.withTooManyArgs()")));
            assertFalse(targets.stream().anyMatch(target -> target.getInvokeTarget().equals("sampleJob.plain()")));
        }
    }

    @Test
    void validateInvokeTarget_shouldKeepSupportingExistingNoArgBeanMethods() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);

            util.validateInvokeTarget("sampleJob.plain()");
        }
    }

    @Test
    void validateInvokeTarget_shouldRejectInvalidFormat() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);

            ApiException exception = assertThrows(ApiException.class,
                () -> util.validateInvokeTarget("sampleJob.clean"));

            assertSame(ErrorCode.Business.JOB_INVOKE_TARGET_INVALID, exception.getErrorCode());
        }
    }

    @Test
    void invoke_shouldCallConfiguredNoArgBeanMethod() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);
            SampleJob sampleJob = context.getBean(SampleJob.class);

            util.invoke("sampleJob.clean()");

            assertEquals(1, sampleJob.cleanCount);
        }
    }

    @Test
    void invoke_shouldCallConfiguredBeanMethodWithJsonParams() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);
            SampleJob sampleJob = context.getBean(SampleJob.class);

            util.invoke("sampleJob.withParams()", "{\"retentionDays\":60}");

            assertEquals(60, sampleJob.params.retentionDays);
        }
    }

    @Test
    void validateInvokeTarget_shouldRejectInvalidJsonParams() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);

            ApiException exception = assertThrows(ApiException.class,
                () -> util.validateInvokeTarget("sampleJob.withParams()", "{\"retentionDays\":\"abc\"}"));

            assertSame(ErrorCode.Business.JOB_PARAMS_INVALID, exception.getErrorCode());
        }
    }

    @Test
    void validateInvokeTarget_shouldRejectMalformedJsonParamsForNoArgMethod() {
        try (AnnotationConfigApplicationContext context = newContext()) {
            JobInvokeUtil util = new JobInvokeUtil(context);

            ApiException exception = assertThrows(ApiException.class,
                () -> util.validateInvokeTarget("sampleJob.clean()", "{bad-json"));

            assertSame(ErrorCode.Business.JOB_PARAMS_INVALID, exception.getErrorCode());
        }
    }

    private AnnotationConfigApplicationContext newContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean("sampleJob", SampleJob.class);
        context.refresh();
        return context;
    }

    static class SampleJob {

        private int cleanCount;
        private SampleParams params;

        @JobTask(name = "Clean data", group = "maintenance", description = "Remove expired data")
        public void clean() {
            cleanCount++;
        }

        @Scheduled(cron = "0 * * * * *")
        public void report() {
        }

        @JobTask
        public void withParams(SampleParams params) {
            this.params = params;
        }

        @JobTask
        public void withTooManyArgs(String value, String other) {
        }

        public void plain() {
        }
    }

    static class SampleParams {

        private int retentionDays;

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
}
