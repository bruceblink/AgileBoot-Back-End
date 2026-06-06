package app.keystone.domain.system.dict.model;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import app.keystone.domain.system.dict.db.SysDictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 字典类型模型工厂
 * @author likanug
 */
@Component
@RequiredArgsConstructor
public class DictTypeModelFactory {

    private final SysDictTypeService dictTypeService;

    public DictTypeModel loadById(Long dictId) {
        SysDictTypeEntity entity = dictTypeService.getById(dictId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, dictId, "字典类型");
        }
        return new DictTypeModel(entity, dictTypeService);
    }

    public DictTypeModel create() {
        return new DictTypeModel(dictTypeService);
    }
}
