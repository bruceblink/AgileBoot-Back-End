package app.keystone.domain.system.job.db;

import app.keystone.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Scheduled job table.
 * @author likanug
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("sys_job")
@ApiModel(value = "SysJobEntity对象", description = "定时任务表")
public class SysJobEntity extends BaseEntity<SysJobEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("任务ID")
    @TableId(value = "job_id", type = IdType.AUTO)
    private Long jobId;

    @ApiModelProperty("任务名称")
    @TableField("job_name")
    private String jobName;

    @ApiModelProperty("任务组名")
    @TableField("job_group")
    private String jobGroup;

    @ApiModelProperty("调用目标")
    @TableField("invoke_target")
    private String invokeTarget;

    @ApiModelProperty("任务参数JSON")
    @TableField("job_params")
    private String jobParams;

    @ApiModelProperty("Cron执行表达式")
    @TableField("cron_expression")
    private String cronExpression;

    @ApiModelProperty("是否允许并发执行（1允许 0禁止）")
    @TableField("concurrent")
    private Integer concurrent;

    @ApiModelProperty("状态（1正常 0暂停）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.jobId;
    }
}
