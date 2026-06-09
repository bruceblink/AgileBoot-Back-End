package app.keystone.domain.system.dict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.enums.dictionary.DictionaryData;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.common.cache.SpringCacheService;
import app.keystone.domain.system.dict.command.AddDictDataCommand;
import app.keystone.domain.system.dict.db.SysDictDataEntity;
import app.keystone.domain.system.dict.db.SysDictDataService;
import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import app.keystone.domain.system.dict.db.SysDictTypeService;
import app.keystone.domain.system.dict.model.DictTypeModelFactory;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.cache.aop.CacheNameConstants;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

class DictApplicationServiceTest {

    private SysDictTypeService dictTypeService;
    private SysDictDataService dictDataService;
    private DictApplicationService service;

    @BeforeEach
    void setUp() {
        dictTypeService = mock(SysDictTypeService.class);
        dictDataService = mock(SysDictDataService.class);
        service = new DictApplicationService(mock(DictTypeModelFactory.class), dictTypeService, dictDataService);

        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new ConcurrentMapCache(CacheNameConstants.DICT_DATA),
            new ConcurrentMapCache(CacheNameConstants.DICTIONARY_DATA_MAP)
        ));
        cacheManager.afterPropertiesSet();
        SpringCacheService springCacheService = new SpringCacheService(cacheManager,
            mock(SysUserService.class), mock(SysRoleService.class), mock(SysPostService.class), dictDataService);
        new CacheCenter(mock(LocalCacheService.class), mock(RedisCacheService.class), springCacheService);
    }

    @Test
    void getDictionaryDataMap_shouldBuildFrontendDictionaryFromEnabledDatabaseRows() {
        when(dictTypeService.list(ArgumentMatchers.<Wrapper<SysDictTypeEntity>>any())).thenReturn(List.of(
            dictType(1L, "common.status"),
            dictType(2L, "sysUser.status"),
            dictType(3L, "sysUser.sex")
        ));
        when(dictDataService.list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any())).thenReturn(List.of(
            dictData("common.status", "正常", "1", 1, ""),
            dictData("common.status", "停用", "0", 2, "danger"),
            dictData("sysUser.status", "冻结", "3", 3, "warning"),
            dictData("sysUser.sex", "女", "0", 1, ""),
            dictData("sysUser.sex", "男", "1", 2, ""),
            dictData("sysUser.sex", "未知", "2", 3, "")
        ));

        Map<String, List<DictionaryData>> dictionary = service.getDictionaryDataMap();

        assertEquals(List.of("common.status", "sysUser.status", "sysUser.sex"), List.copyOf(dictionary.keySet()));
        assertEquals("正常", dictionary.get("common.status").get(0).getLabel());
        assertEquals(1, dictionary.get("common.status").get(0).getValue());
        assertEquals("danger", dictionary.get("common.status").get(1).getCssTag());
        assertEquals("冻结", dictionary.get("sysUser.status").get(0).getLabel());
        assertEquals("女", dictionary.get("sysUser.sex").get(0).getLabel());
        assertEquals(0, dictionary.get("sysUser.sex").get(0).getValue());
        assertEquals("男", dictionary.get("sysUser.sex").get(1).getLabel());
        assertEquals(1, dictionary.get("sysUser.sex").get(1).getValue());
        assertEquals("未知", dictionary.get("sysUser.sex").get(2).getLabel());
        assertEquals(2, dictionary.get("sysUser.sex").get(2).getValue());
    }

    @Test
    void getDictionaryDataMap_shouldUseCacheAfterFirstLoad() {
        when(dictTypeService.list(ArgumentMatchers.<Wrapper<SysDictTypeEntity>>any())).thenReturn(List.of(
            dictType(1L, "common.status")
        ));
        when(dictDataService.list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any())).thenReturn(List.of(
            dictData("common.status", "正常", "1", 1, "")
        ));

        service.getDictionaryDataMap();
        service.getDictionaryDataMap();

        verify(dictTypeService, times(1)).list(ArgumentMatchers.<Wrapper<SysDictTypeEntity>>any());
        verify(dictDataService, times(1)).list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any());
    }

    @Test
    void addDictData_shouldInvalidateCachedDictionaryDataMap() {
        when(dictTypeService.list(ArgumentMatchers.<Wrapper<SysDictTypeEntity>>any())).thenReturn(List.of(
            dictType(1L, "common.status")
        ));
        when(dictDataService.list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any()))
            .thenReturn(List.of(dictData("common.status", "正常", "1", 1, "")))
            .thenReturn(List.of(dictData("common.status", "停用", "0", 2, "danger")));
        AddDictDataCommand command = new AddDictDataCommand();
        command.setDictType("common.status");
        command.setDictLabel("停用");
        command.setDictValue("0");
        command.setStatus(1);

        service.getDictionaryDataMap();
        service.addDictData(command);
        Map<String, List<DictionaryData>> dictionary = service.getDictionaryDataMap();

        assertEquals("停用", dictionary.get("common.status").get(0).getLabel());
        verify(dictDataService, times(2)).list(ArgumentMatchers.<Wrapper<SysDictDataEntity>>any());
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
