package app.keystone.domain.system.log.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.log.db.SysOperationLogEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    private String status;
    private String username;
    private String requestModule;

    @Override
    public QueryWrapper<SysOperationLogEntity> addQueryCondition() {
        QueryWrapper<SysOperationLogEntity> queryWrapper = new QueryWrapper<SysOperationLogEntity>()
            .like(businessType!=null, "business_type", businessType)
            .eq(status != null, "status", status)
            .like(StringUtils.isNotEmpty(username), "username", username)
            .like(StringUtils.isNotEmpty(requestModule), "request_module", requestModule);

        this.timeRangeColumn = "operation_time";

        return queryWrapper;
    }
}
