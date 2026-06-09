package app.keystone.domain.system.notice.model;

import app.keystone.domain.system.notice.command.NoticeAddCommand;
import app.keystone.domain.system.notice.command.NoticeUpdateCommand;
import app.keystone.common.enums.common.NoticeTypeEnum;
import app.keystone.common.enums.common.StatusEnum;
import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.domain.system.notice.db.SysNoticeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class NoticeModel extends SysNoticeEntity {

    public NoticeModel(SysNoticeEntity entity) {
        if (entity != null) {
            BeanUtils.copyProperties(entity, this);
        }
    }

    public void loadAddCommand(NoticeAddCommand command) {
        if (command != null) {
            BeanUtils.copyProperties(command, this, "noticeId");
        }
    }

    public void loadUpdateCommand(NoticeUpdateCommand command) {
        if (command != null) {
            loadAddCommand(command);
        }
    }

    public void checkFields() {
        BasicEnumUtil.fromValue(NoticeTypeEnum.class, getNoticeType());
        BasicEnumUtil.fromValue(StatusEnum.class, getStatus());
    }

}
