package app.keystone.domain.system.notice.command;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NoticeUpdateCommand extends NoticeAddCommand {

    @Positive(message = "公告ID必须为正数")
    protected Long noticeId;

}
