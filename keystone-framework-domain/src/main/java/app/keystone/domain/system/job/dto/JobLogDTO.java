package app.keystone.domain.system.job.dto;

import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.common.enums.common.JobLogStatusEnum;
import app.keystone.common.enums.common.JobTriggerTypeEnum;
import app.keystone.domain.system.job.db.SysJobLogEntity;
import java.util.Date;
import lombok.Data;

/**
 * Scheduled job execution log DTO.
 *
 * @author likanug
 */
@Data
public class JobLogDTO {

    public JobLogDTO(SysJobLogEntity entity) {
        if (entity != null) {
            jobLogId = entity.getJobLogId();
            jobId = entity.getJobId();
            jobName = entity.getJobName();
            jobGroup = entity.getJobGroup();
            invokeTarget = entity.getInvokeTarget();
            cronExpression = entity.getCronExpression();
            triggerType = entity.getTriggerType();
            triggerTypeStr = BasicEnumUtil.getDescriptionByValue(JobTriggerTypeEnum.class, entity.getTriggerType());
            status = entity.getStatus();
            statusStr = BasicEnumUtil.getDescriptionByValue(JobLogStatusEnum.class, entity.getStatus());
            jobMessage = entity.getJobMessage();
            exceptionInfo = entity.getExceptionInfo();
            startTime = entity.getStartTime();
            endTime = entity.getEndTime();
            durationMs = entity.getDurationMs();
            createTime = entity.getCreateTime();
        }
    }

    private Long jobLogId;
    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer triggerType;
    private String triggerTypeStr;
    private Integer status;
    private String statusStr;
    private String jobMessage;
    private String exceptionInfo;
    private Date startTime;
    private Date endTime;
    private Long durationMs;
    private Date createTime;
}
