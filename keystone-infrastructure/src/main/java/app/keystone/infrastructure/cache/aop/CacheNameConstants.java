package app.keystone.infrastructure.cache.aop;

/**
 * @author valarchie
 */
public class CacheNameConstants {

    public static final String CAFFEINE = "caffeine";

    /** @deprecated 已迁移至 Caffeine，请使用 {@link #CAFFEINE} */
    @Deprecated
    public static final String GUAVA = "guava";

    public static final String REDIS = "redis";

    public static final String USER_ENTITY = "userEntity";

    public static final String ROLE_ENTITY = "roleEntity";

    public static final String POST_ENTITY = "postEntity";

    public static final String DICT_DATA = "dictData";

    public static final String DEVICE_LIST_QUERY = "deviceListQuery";

    private CacheNameConstants() {
    }
}
