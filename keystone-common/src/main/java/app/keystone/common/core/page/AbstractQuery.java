package app.keystone.common.core.page;

import app.keystone.common.utils.sql.SqlLikeUtils;
import app.keystone.common.utils.time.DatePickUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.util.Date;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 如果是简单的排序 和 时间范围筛选  可以使用内置的这几个字段
 * @author likanug
 */
@Data
public abstract class AbstractQuery<T> {

    protected String orderColumn;

    protected String orderDirection;

    protected String timeRangeColumn;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private Date beginTime;

    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    private Date endTime;

    private static final String ASC = "ascending";
    private static final String DESC = "descending";

    /**
     * 生成query conditions
     *
     * @return 添加条件后的QueryWrapper
     */
    public QueryWrapper<T> toQueryWrapper() {
        QueryWrapper<T> queryWrapper = addQueryCondition();
        addSortCondition(queryWrapper);
        addTimeCondition(queryWrapper);

        return queryWrapper;
    }

    public abstract QueryWrapper<T> addQueryCondition();

    protected String likeValue(String value) {
        return SqlLikeUtils.escape(value);
    }

    public void addSortCondition(QueryWrapper<T> queryWrapper) {
        if (queryWrapper == null || StringUtils.isEmpty(orderColumn)) {
            return;
        }

        Boolean sortDirection = convertSortDirection();
        if (sortDirection != null) {
            queryWrapper.orderBy(StringUtils.isNotEmpty(orderColumn), sortDirection,
                toUnderlineCase(orderColumn));
        }
    }

    public void addTimeCondition(QueryWrapper<T> queryWrapper) {
        if (queryWrapper != null
            && StringUtils.isNotEmpty(this.timeRangeColumn)) {
            queryWrapper
                .ge(beginTime != null, toUnderlineCase(timeRangeColumn),
                    DatePickUtil.getBeginOfTheDay(beginTime))
                .le(endTime != null, toUnderlineCase(timeRangeColumn), DatePickUtil.getEndOfTheDay(endTime));
        }
    }

    /**
     * 获取前端传来的排序方向  转换成MyBatisPlus所需的排序参数 boolean=isAsc
     * @return 排序顺序， null为无排序
     */
    public Boolean convertSortDirection() {
        Boolean isAsc = null;
        if (StringUtils.isEmpty(this.orderDirection)) {
            return isAsc;
        }

        if (ASC.equals(this.orderDirection)) {
            isAsc = true;
        }
        if (DESC.equals(this.orderDirection)) {
            isAsc = false;
        }

        return isAsc;
    }

    private String toUnderlineCase(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

}
