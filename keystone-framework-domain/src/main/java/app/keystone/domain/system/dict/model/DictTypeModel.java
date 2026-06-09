package app.keystone.domain.system.dict.model;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.dict.command.AddDictTypeCommand;
import app.keystone.domain.system.dict.command.UpdateDictTypeCommand;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import app.keystone.domain.system.dict.db.SysDictTypeService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

/**
 * 字典类型领域模型
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DictTypeModel extends SysDictTypeEntity {

    private SysDictTypeService dictTypeService;

    public DictTypeModel(SysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    public DictTypeModel(SysDictTypeEntity entity, SysDictTypeService dictTypeService) {
        BeanUtils.copyProperties(entity, this);
        this.dictTypeService = dictTypeService;
    }

    public void loadAddCommand(AddDictTypeCommand command) {
        BeanUtils.copyProperties(command, this);
    }

    public void loadUpdateCommand(UpdateDictTypeCommand command) {
        String oldDictType = this.getDictType();
        BeanUtils.copyProperties(command, this);
        this.setDictId(command.getDictId());
        // 如果类型发生变更，校验唯一性
        if (!oldDictType.equals(command.getDictType())) {
            checkDictTypeUnique(command.getDictId());
        }
    }

    public void checkDictTypeUnique(Long excludeDictId) {
        if (!dictTypeService.isDictTypeUnique(this.getDictType(), excludeDictId)) {
            throw new ApiException(ErrorCode.Business.DICT_TYPE_IS_NOT_UNIQUE, this.getDictType());
        }
    }
}
