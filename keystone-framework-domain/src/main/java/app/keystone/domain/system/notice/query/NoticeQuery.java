package app.keystone.domain.system.notice.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.notice.db.SysNoticeEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class NoticeQuery extends AbstractPageQuery<SysNoticeEntity> {

    @Pattern(regexp = "^[1-2]$", message = "公告类型值无效")
    private String noticeType;

    private String noticeTitle;

    private String creatorName;


    @Override
    public QueryWrapper<SysNoticeEntity> addQueryCondition() {
        QueryWrapper<SysNoticeEntity> queryWrapper = new QueryWrapper<SysNoticeEntity>()
            .like(StringUtils.isNotEmpty(noticeTitle), "notice_title", likeValue(noticeTitle))
            .eq(StringUtils.isNotEmpty(noticeType), "notice_type", noticeType)
            .like(StringUtils.isNotEmpty(creatorName), "u.username", likeValue(creatorName));
        return queryWrapper;
    }
}
