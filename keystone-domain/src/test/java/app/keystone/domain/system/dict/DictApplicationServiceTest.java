package app.keystone.domain.system.dict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.enums.dictionary.DictionaryData;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.dict.db.SysDictDataService;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import app.keystone.domain.system.dict.db.SysDictTypeService;
import app.keystone.domain.system.dict.model.DictTypeModelFactory;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class DictApplicationServiceTest {

    @Test
    void getDictionaryDataMap_shouldBuildFrontendDictionaryFromEnabledDatabaseRows() {
        SysDictTypeService dictTypeService = mock(SysDictTypeService.class);
        SysDictDataService dictDataService = mock(SysDictDataService.class);
        DictApplicationService service = new DictApplicationService(
            mock(DictTypeModelFactory.class), dictTypeService, dictDataService);

        when(dictTypeService.list(ArgumentMatchers.<Wrapper<SysDictTypeEntity>>any())).thenReturn(List.of(
            dictType(1L, "common.status"),
            dictType(2L, "sysUser.status")
        ));
        when(dictDataService.list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any())).thenReturn(List.of(
            dictData("common.status", "正常", "1", 1, ""),
            dictData("common.status", "停用", "0", 2, "danger"),
            dictData("sysUser.status", "冻结", "3", 3, "warning")
        ));

        Map<String, List<DictionaryData>> dictionary = service.getDictionaryDataMap();

        assertEquals(List.of("common.status", "sysUser.status"), List.copyOf(dictionary.keySet()));
        assertEquals("正常", dictionary.get("common.status").get(0).getLabel());
        assertEquals(1, dictionary.get("common.status").get(0).getValue());
        assertEquals("danger", dictionary.get("common.status").get(1).getCssTag());
        assertEquals("冻结", dictionary.get("sysUser.status").get(0).getLabel());
    }

    private SysDictTypeEntity dictType(Long id, String type) {
        SysDictTypeEntity entity = new SysDictTypeEntity();
        entity.setDictId(id);
        entity.setDictType(type);
        entity.setStatus(1);
        return entity;
    }

    private SysDictDataEntity dictData(String type, String label, String value, Integer sort, String cssTag) {
        SysDictDataEntity entity = new SysDictDataEntity();
        entity.setDictType(type);
        entity.setDictLabel(label);
        entity.setDictValue(value);
        entity.setDictSort(sort);
        entity.setListClass(cssTag);
        entity.setStatus(1);
        return entity;
    }
}
