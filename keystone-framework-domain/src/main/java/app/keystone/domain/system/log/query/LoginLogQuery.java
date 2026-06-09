package app.keystone.domain.system.log.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.log.db.SysLoginInfoEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
    private String status;
    private String username;


    @Override
    public QueryWrapper<SysLoginInfoEntity> addQueryCondition() {
        QueryWrapper<SysLoginInfoEntity> queryWrapper = new QueryWrapper<SysLoginInfoEntity>()
            .like(StringUtils.isNotEmpty(ipAddress), "ip_address", ipAddress)
            .eq(StringUtils.isNotEmpty(status), "status", status)
            .like(StringUtils.isNotEmpty(username), "username", username);

        addSortCondition(queryWrapper);

        // 可以手动设置  也可以由前端传回
//        this.timeRangeColumn = "login_time";
//        addTimeCondition(queryWrapper, "login_time");

        return queryWrapper;
    }
}
