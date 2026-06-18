package app.keystone.domain.system.job.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.job.db.SysJobLogEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * Scheduled job execution log query.
 *
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(name = "定时任务运行日志查询参数")
public class JobLogQuery extends AbstractPageQuery<SysJobLogEntity> {

    @Schema(description = "任务ID")
    private Long jobId;

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务组名")
    private String jobGroup;

    @Schema(description = "调用目标")
    private String invokeTarget;

    @Schema(description = "触发类型（1自动调度 2手动执行）")
    @Min(value = 1, message = "触发类型值无效")
    @Max(value = 2, message = "触发类型值无效")
    private Integer triggerType;

    @Schema(description = "执行状态（1成功 0失败 2跳过）")
    @Min(value = 0, message = "执行状态值无效")
    @Max(value = 2, message = "执行状态值无效")
    private Integer status;

    @Override
    public QueryWrapper<SysJobLogEntity> addQueryCondition() {
        this.timeRangeColumn = "start_time";
        return new QueryWrapper<SysJobLogEntity>()
            .eq(jobId != null, "job_id", jobId)
            .like(StringUtils.isNotEmpty(jobName), "job_name", likeValue(jobName))
            .like(StringUtils.isNotEmpty(jobGroup), "job_group", likeValue(jobGroup))
            .like(StringUtils.isNotEmpty(invokeTarget), "invoke_target", likeValue(invokeTarget))
            .eq(triggerType != null, "trigger_type", triggerType)
            .eq(status != null, "status", status);
    }
}
