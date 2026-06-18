package app.keystone.domain.system.job.db;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * Scheduled job execution log table.
 *
 * @author likanug
 */
@Getter
@Setter
@TableName("sys_job_log")
@ApiModel(value = "SysJobLogEntity对象", description = "定时任务运行日志表")
public class SysJobLogEntity extends Model<SysJobLogEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("任务日志ID")
    @TableId(value = "job_log_id", type = IdType.AUTO)
    private Long jobLogId;

    @ApiModelProperty("任务ID")
    @TableField("job_id")
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

    @ApiModelProperty("Cron执行表达式")
    @TableField("cron_expression")
    private String cronExpression;

    @ApiModelProperty("触发类型（1自动调度 2手动执行）")
    @TableField("trigger_type")
    private Integer triggerType;

    @ApiModelProperty("执行状态（1成功 0失败 2跳过）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("日志信息")
    @TableField("job_message")
    private String jobMessage;

    @ApiModelProperty("异常信息")
    @TableField("exception_info")
    private String exceptionInfo;

    @ApiModelProperty("开始时间")
    @TableField("start_time")
    private Date startTime;

    @ApiModelProperty("结束时间")
    @TableField("end_time")
    private Date endTime;

    @ApiModelProperty("耗时毫秒")
    @TableField("duration_ms")
    private Long durationMs;

    @ApiModelProperty("创建时间")
    @TableField("create_time")
    private Date createTime;

    @Override
    public Serializable pkVal() {
        return this.jobLogId;
    }
}
