package app.keystone.domain.common.command;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public final class BulkOperationCommand<T> {

    public BulkOperationCommand(List<T> idList) {
        if (idList == null || idList.isEmpty()) {
            throw new ApiException(ErrorCode.Business.COMMON_BULK_DELETE_IDS_IS_INVALID);
        }
        // 移除重复元素
        this.ids = new HashSet<>(idList);
    }

    private Set<T> ids;

}
