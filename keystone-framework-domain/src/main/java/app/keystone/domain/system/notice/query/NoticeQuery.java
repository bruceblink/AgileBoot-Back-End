package app.keystone.domain.system.notice.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.notice.db.SysNoticeEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    private String noticeType;

    private String noticeTitle;

    private String creatorName;


    @Override
    public QueryWrapper<SysNoticeEntity> addQueryCondition() {
        QueryWrapper<SysNoticeEntity> queryWrapper = new QueryWrapper<SysNoticeEntity>()
            .like(StringUtils.isNotEmpty(noticeTitle), "notice_title", likeValue(noticeTitle))
            .eq(StringUtils.isNotEmpty(noticeType), "notice_type", noticeType)
            .eq("n.deleted", 0)
            .like(StringUtils.isNotEmpty(creatorName), "u.username", likeValue(creatorName));
        return queryWrapper;
    }
}
