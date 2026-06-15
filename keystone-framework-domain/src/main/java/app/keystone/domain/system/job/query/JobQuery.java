package app.keystone.domain.system.job.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.job.db.SysJobEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Scheduled job query.
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Schema(name = "定时任务查询参数")
public class JobQuery extends AbstractPageQuery<SysJobEntity> {

    @Schema(description = "任务名称")
    private String jobName;

    @Schema(description = "任务组名")
    private String jobGroup;

    @Schema(description = "状态")
    private Integer status;

    @Override
    public QueryWrapper<SysJobEntity> addQueryCondition() {
        this.timeRangeColumn = "create_time";
        return new QueryWrapper<SysJobEntity>()
            .like(StringUtils.isNotEmpty(jobName), "job_name", likeValue(jobName))
            .like(StringUtils.isNotEmpty(jobGroup), "job_group", likeValue(jobGroup))
            .eq(status != null, "status", status);
    }
}
