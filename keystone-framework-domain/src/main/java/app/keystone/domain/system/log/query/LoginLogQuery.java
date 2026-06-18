package app.keystone.domain.system.log.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.log.db.SysLoginInfoEntity;
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
public class LoginLogQuery extends AbstractPageQuery<SysLoginInfoEntity> {

    private String ipAddress;
    @Pattern(regexp = "^[0-3]$", message = "登录状态值无效")
    private String status;
    private String username;


    @Override
    public QueryWrapper<SysLoginInfoEntity> addQueryCondition() {
        QueryWrapper<SysLoginInfoEntity> queryWrapper = new QueryWrapper<SysLoginInfoEntity>()
            .like(StringUtils.isNotEmpty(ipAddress), "ip_address", likeValue(ipAddress))
            .eq(StringUtils.isNotEmpty(status), "status", status)
            .like(StringUtils.isNotEmpty(username), "username", likeValue(username));

        this.timeRangeColumn = "login_time";
        if (StringUtils.isEmpty(orderColumn) || StringUtils.isEmpty(orderDirection)) {
            queryWrapper.orderByDesc("login_time", "info_id");
        }

        return queryWrapper;
    }

    @Override
    public void addSortCondition(QueryWrapper<SysLoginInfoEntity> queryWrapper) {
        super.addSortCondition(queryWrapper);
        if (queryWrapper != null && ("loginTime".equals(orderColumn) || "login_time".equals(orderColumn))) {
            queryWrapper.orderByDesc("info_id");
        }
    }
}
