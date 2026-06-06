package app.keystone.domain.common.cache;

import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.common.enums.common.GenderEnum;
import app.keystone.common.enums.common.JobStatusEnum;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.enums.common.NoticeStatusEnum;
import app.keystone.common.enums.common.NoticeTypeEnum;
import app.keystone.common.enums.common.OperationStatusEnum;
import app.keystone.common.enums.common.StatusEnum;
import app.keystone.common.enums.common.UserStatusEnum;
import app.keystone.common.enums.common.VisibleStatusEnum;
import app.keystone.common.enums.common.YesOrNoEnum;
import app.keystone.common.enums.dictionary.Dictionary;
import app.keystone.common.enums.DictionaryEnum;
import app.keystone.common.enums.dictionary.DictionaryData;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 本地一级缓存  使用Map
 *
 * @author likanug
 */
public class MapCache {

    private static final Map<String, List<DictionaryData>> DICTIONARY_CACHE = new HashMap<>(128);

    private MapCache() {
    }

    static {
        initDictionaryCache();
    }

    @SuppressWarnings("deprecation")
    private static void initDictionaryCache() {
        loadInCache(BusinessTypeEnum.values());
        loadInCache(YesOrNoEnum.values());
        loadInCache(StatusEnum.values());
        loadInCache(GenderEnum.values());
        loadInCache(NoticeStatusEnum.values());
        loadInCache(NoticeTypeEnum.values());
        loadInCache(OperationStatusEnum.values());
        loadInCache(LoginStatusEnum.values());
        loadInCache(VisibleStatusEnum.values());
        loadInCache(UserStatusEnum.values());
        loadInCache(JobStatusEnum.values());
    }


    public static Map<String, List<DictionaryData>> dictionaryCache() {
        return Collections.unmodifiableMap(DICTIONARY_CACHE);
    }

    private static void loadInCache(DictionaryEnum[] dictionaryEnums) {
        DICTIONARY_CACHE.put(getDictionaryName(dictionaryEnums[0].getClass()), arrayToList(dictionaryEnums));
    }


    private static String getDictionaryName(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        Dictionary annotation = clazz.getAnnotation(Dictionary.class);

        Objects.requireNonNull(annotation);
        return annotation.name();
    }

    @SuppressWarnings("rawtypes")
    private static List<DictionaryData> arrayToList(DictionaryEnum[] dictionaryEnums) {
        if (dictionaryEnums == null || dictionaryEnums.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(dictionaryEnums).map(DictionaryData::new).collect(Collectors.toList());
    }


}
