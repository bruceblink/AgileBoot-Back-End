package app.keystone.domain.system.job;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.system.job.command.AddJobCommand;
import app.keystone.domain.system.job.command.UpdateJobCommand;
import app.keystone.domain.system.job.command.UpdateJobStatusCommand;
import app.keystone.domain.system.job.db.SysJobEntity;
import app.keystone.domain.system.job.db.SysJobService;
import app.keystone.domain.system.job.dto.JobDTO;
import app.keystone.domain.system.job.query.JobQuery;
import app.keystone.domain.system.job.runtime.JobInvokeUtil;
import app.keystone.domain.system.job.runtime.JobSchedulerManager;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job application service.
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private final SysJobService jobService;
    private final JobSchedulerManager jobSchedulerManager;
    private final JobInvokeUtil jobInvokeUtil;

    public PageDTO<JobDTO> getJobList(JobQuery query) {
        Page<SysJobEntity> page = jobService.page(query.toPage(), query.toQueryWrapper());
        List<JobDTO> records = page.getRecords().stream().map(JobDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public JobDTO getJobInfo(Long jobId) {
        return new JobDTO(loadJob(jobId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void addJob(AddJobCommand command) {
        validateJob(command);
        SysJobEntity entity = new SysJobEntity();
        BeanUtils.copyProperties(command, entity);
        jobService.save(entity);
        jobSchedulerManager.schedule(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateJob(UpdateJobCommand command) {
        validateJob(command);
        SysJobEntity entity = loadJob(command.getJobId());
        BeanUtils.copyProperties(command, entity);
        jobService.updateById(entity);
        jobSchedulerManager.schedule(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateJobStatus(Long jobId, UpdateJobStatusCommand command) {
        BasicEnumUtil.fromValue(JobStatusEnum.class, command.getStatus());
        SysJobEntity entity = loadJob(jobId);
        entity.setStatus(command.getStatus());
        jobService.updateById(entity);
        jobSchedulerManager.schedule(entity);
    }

    public void runJobOnce(Long jobId) {
        SysJobEntity entity = loadJob(jobId);
        validateJob(entity);
        jobSchedulerManager.runOnce(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteJobs(BulkOperationCommand<Long> command) {
        command.getIds().forEach(jobSchedulerManager::cancel);
        jobService.removeBatchByIds(command.getIds());
    }

    private SysJobEntity loadJob(Long jobId) {
        SysJobEntity entity = jobService.getById(jobId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, jobId, "定时任务");
        }
        return entity;
    }

    private void validateJob(AddJobCommand command) {
        BasicEnumUtil.fromValue(JobStatusEnum.class, command.getStatus());
        BasicEnumUtil.fromValue(YesOrNoEnum.class, command.getConcurrent());
        validateCron(command.getCronExpression());
        jobInvokeUtil.validateInvokeTarget(command.getInvokeTarget());
    }

    private void validateJob(SysJobEntity entity) {
        validateCron(entity.getCronExpression());
        jobInvokeUtil.validateInvokeTarget(entity.getInvokeTarget());
    }

    private void validateCron(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
        } catch (IllegalArgumentException e) {
            throw new ApiException(e, ErrorCode.Business.JOB_CRON_EXPRESSION_INVALID, cronExpression);
        }
    }
}
