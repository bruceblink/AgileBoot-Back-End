package app.keystone.domain.system.log.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.log.db.SysOperationLogEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OperationLogQuery extends AbstractPageQuery<SysOperationLogEntity> {

    private String businessType;
    @Pattern(regexp = "^[0-1]$", message = "操作状态值无效")
    private String status;
    private String username;
    private String requestModule;

    @Override
    public QueryWrapper<SysOperationLogEntity> addQueryCondition() {
        QueryWrapper<SysOperationLogEntity> queryWrapper = new QueryWrapper<SysOperationLogEntity>()
            .like(businessType != null, "business_type", likeValue(businessType))
            .eq(status != null, "status", status)
            .like(StringUtils.isNotEmpty(username), "username", likeValue(username))
            .like(StringUtils.isNotEmpty(requestModule), "request_module", likeValue(requestModule));

        this.timeRangeColumn = "operation_time";
        if (StringUtils.isEmpty(orderColumn) || StringUtils.isEmpty(orderDirection)) {
            queryWrapper.orderByDesc("operation_time", "operation_id");
        }

        return queryWrapper;
    }

    @Override
    public void addSortCondition(QueryWrapper<SysOperationLogEntity> queryWrapper) {
        super.addSortCondition(queryWrapper);
        if (queryWrapper != null && ("operationTime".equals(orderColumn) || "operation_time".equals(orderColumn))) {
            queryWrapper.orderByDesc("operation_id");
        }
    }
}
