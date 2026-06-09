package app.keystone.domain.system.job.dto;

import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.domain.system.job.db.SysJobEntity;
import java.util.Date;
import lombok.Data;

/**
 * Scheduled job DTO.
 * @author likanug
 */
@Data
public class JobDTO {

    public JobDTO(SysJobEntity entity) {
        if (entity != null) {
            jobId = entity.getJobId();
            jobName = entity.getJobName();
            jobGroup = entity.getJobGroup();
            invokeTarget = entity.getInvokeTarget();
            cronExpression = entity.getCronExpression();
            concurrent = entity.getConcurrent();
            concurrentStr = BasicEnumUtil.getDescriptionByValue(YesOrNoEnum.class, entity.getConcurrent());
            status = entity.getStatus();
            statusStr = BasicEnumUtil.getDescriptionByValue(JobStatusEnum.class, entity.getStatus());
            remark = entity.getRemark();
            createTime = entity.getCreateTime();
        }
    }

    private Long jobId;
    private String jobName;
    private String jobGroup;
    private String invokeTarget;
    private String cronExpression;
    private Integer concurrent;
    private String concurrentStr;
    private Integer status;
    private String statusStr;
    private String remark;
    private Date createTime;
}
