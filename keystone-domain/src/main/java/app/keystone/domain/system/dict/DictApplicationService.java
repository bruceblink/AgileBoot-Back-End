package app.keystone.domain.system.dict;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.dictionary.DictionaryData;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.system.dict.command.AddDictDataCommand;
import app.keystone.domain.system.dict.command.AddDictTypeCommand;
import app.keystone.domain.system.dict.command.UpdateDictDataCommand;
import app.keystone.domain.system.dict.command.UpdateDictTypeCommand;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.dict.db.SysDictDataService;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import app.keystone.domain.system.dict.db.SysDictTypeService;
import app.keystone.domain.system.dict.dto.DictDataDTO;
import app.keystone.domain.system.dict.dto.DictTypeDTO;
import app.keystone.domain.system.dict.model.DictTypeModel;
import app.keystone.domain.system.dict.model.DictTypeModelFactory;
import app.keystone.domain.system.dict.query.DictDataQuery;
import app.keystone.domain.system.dict.query.DictTypeQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 字典应用服务
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class DictApplicationService {

    private final DictTypeModelFactory dictTypeModelFactory;
    private final SysDictTypeService dictTypeService;
    private final SysDictDataService dictDataService;

    // ======================== 字典类型 ========================

    public PageDTO<DictTypeDTO> getDictTypeList(DictTypeQuery query) {
        Page<SysDictTypeEntity> page = dictTypeService.page(query.toPage(), query.toQueryWrapper());
        List<DictTypeDTO> records = page.getRecords().stream().map(DictTypeDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public DictTypeDTO getDictTypeInfo(Long dictId) {
        SysDictTypeEntity entity = dictTypeService.getById(dictId);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, dictId, "字典类型");
        }
        return new DictTypeDTO(entity);
    }

    public void addDictType(AddDictTypeCommand command) {
        DictTypeModel model = dictTypeModelFactory.create();
        model.loadAddCommand(command);
        model.checkDictTypeUnique(null);
        model.insert();
    }

    public void updateDictType(UpdateDictTypeCommand command) {
        DictTypeModel model = dictTypeModelFactory.loadById(command.getDictId());
        String oldDictType = model.getDictType();
        model.loadUpdateCommand(command);
        model.updateById();
        // 若字典类型标识变更，同步更新字典数据中的 dictType 并刷新缓存
        if (!oldDictType.equals(command.getDictType())) {
            dictDataService.lambdaUpdate()
                .eq(SysDictDataEntity::getDictType, oldDictType)
                .set(SysDictDataEntity::getDictType, command.getDictType())
                .update();
            CacheCenter.dictDataCache().delete(oldDictType);
        }
        CacheCenter.dictDataCache().delete(command.getDictType());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long dictId) {
        DictTypeModel model = dictTypeModelFactory.loadById(dictId);
        long dataCount = dictDataService.lambdaQuery()
            .eq(SysDictDataEntity::getDictType, model.getDictType())
            .count();
        if (dataCount > 0) {
            throw new ApiException(ErrorCode.Business.DICT_TYPE_HAS_DATA_NOT_ALLOW_DELETE);
        }
        CacheCenter.dictDataCache().delete(model.getDictType());
        model.deleteById();
    }

    // ======================== 字典数据 ========================

    public PageDTO<DictDataDTO> getDictDataList(DictDataQuery query) {
        Page<SysDictDataEntity> page = dictDataService.page(query.toPage(), query.toQueryWrapper());
        List<DictDataDTO> records = page.getRecords().stream().map(DictDataDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public DictDataDTO getDictDataInfo(Long dictCode) {
        SysDictDataEntity entity = dictDataService.getById(dictCode);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, dictCode, "字典数据");
        }
        return new DictDataDTO(entity);
    }

    public List<DictDataDTO> getDictDataByType(String dictType) {
        List<SysDictDataEntity> list = CacheCenter.dictDataCache().getObjectById(dictType);
        return list.stream().map(DictDataDTO::new).collect(Collectors.toList());
    }

    public Map<String, List<DictionaryData>> getDictionaryDataMap() {
        List<SysDictTypeEntity> dictTypeList = dictTypeService.list(new LambdaQueryWrapper<SysDictTypeEntity>()
            .eq(SysDictTypeEntity::getStatus, 1)
            .orderByAsc(SysDictTypeEntity::getDictId));
        if (dictTypeList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> dictTypes = dictTypeList.stream()
            .map(SysDictTypeEntity::getDictType)
            .collect(Collectors.toList());
        List<SysDictDataEntity> dictDataList = dictDataService.list(new LambdaQueryWrapper<SysDictDataEntity>()
            .in(SysDictDataEntity::getDictType, dictTypes)
            .eq(SysDictDataEntity::getStatus, 1)
            .orderByAsc(SysDictDataEntity::getDictType)
            .orderByAsc(SysDictDataEntity::getDictSort));

        Map<String, List<DictionaryData>> dataByType = dictDataList.stream()
            .collect(Collectors.groupingBy(SysDictDataEntity::getDictType, LinkedHashMap::new,
                Collectors.mapping(this::toDictionaryData, Collectors.toList())));
        Map<String, List<DictionaryData>> dictionary = new LinkedHashMap<>();
        for (SysDictTypeEntity dictType : dictTypeList) {
            dictionary.put(dictType.getDictType(),
                dataByType.getOrDefault(dictType.getDictType(), Collections.emptyList()));
        }
        return dictionary;
    }

    public void addDictData(AddDictDataCommand command) {
        SysDictDataEntity entity = new SysDictDataEntity();
        BeanUtils.copyProperties(command, entity);
        dictDataService.save(entity);
        CacheCenter.dictDataCache().delete(command.getDictType());
    }

    public void updateDictData(UpdateDictDataCommand command) {
        SysDictDataEntity entity = dictDataService.getById(command.getDictCode());
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, command.getDictCode(), "字典数据");
        }
        BeanUtils.copyProperties(command, entity);
        dictDataService.updateById(entity);
        CacheCenter.dictDataCache().delete(command.getDictType());
    }

    public void deleteDictData(Long dictCode) {
        SysDictDataEntity entity = dictDataService.getById(dictCode);
        if (entity == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, dictCode, "字典数据");
        }
        dictDataService.removeById(dictCode);
        CacheCenter.dictDataCache().delete(entity.getDictType());
    }

    private DictionaryData toDictionaryData(SysDictDataEntity entity) {
        try {
            return new DictionaryData(entity.getDictLabel(), Integer.valueOf(entity.getDictValue()), entity.getListClass());
        } catch (NumberFormatException e) {
            throw new ApiException(e, ErrorCode.Internal.INTERNAL_ERROR,
                "字典值必须是整数：" + entity.getDictType() + "=" + entity.getDictValue());
        }
    }
}
