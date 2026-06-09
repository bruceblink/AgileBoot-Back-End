package app.keystone.domain.system.notice;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.system.notice.command.NoticeAddCommand;
import app.keystone.domain.system.notice.command.NoticeUpdateCommand;
import app.keystone.domain.system.notice.dto.NoticeDTO;
import app.keystone.domain.system.notice.model.NoticeModel;
import app.keystone.domain.system.notice.model.NoticeModelFactory;
import app.keystone.domain.system.notice.query.NoticeQuery;
import app.keystone.domain.system.notice.db.SysNoticeEntity;
import app.keystone.domain.system.notice.db.SysNoticeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class NoticeApplicationService {

    private final SysNoticeService noticeService;

    private final NoticeModelFactory noticeModelFactory;

    public PageDTO<NoticeDTO> getNoticeList(NoticeQuery query) {
        Page<SysNoticeEntity> page = noticeService.getNoticeList(query.toPage(), query.toQueryWrapper());
        List<NoticeDTO> records = page.getRecords().stream().map(NoticeDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }


    public NoticeDTO getNoticeInfo(Long id) {
        NoticeModel noticeModel = noticeModelFactory.loadById(id);
        return new NoticeDTO(noticeModel);
    }


    public void addNotice(NoticeAddCommand addCommand) {
        NoticeModel noticeModel = noticeModelFactory.create();
        noticeModel.loadAddCommand(addCommand);

        noticeModel.checkFields();

        noticeModel.insert();
    }


    public void updateNotice(NoticeUpdateCommand updateCommand) {
        NoticeModel noticeModel = noticeModelFactory.loadById(updateCommand.getNoticeId());
        noticeModel.loadUpdateCommand(updateCommand);

        noticeModel.checkFields();

        noticeModel.updateById();
    }

    public void deleteNotice(BulkOperationCommand<Integer> deleteCommand) {
        noticeService.removeBatchByIds(deleteCommand.getIds());
    }




}
