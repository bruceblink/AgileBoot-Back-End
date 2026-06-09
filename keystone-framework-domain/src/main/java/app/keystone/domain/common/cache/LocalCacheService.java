package app.keystone.domain.common.cache;


import app.keystone.infrastructure.cache.caffeine.AbstractCaffeineCacheTemplate;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.domain.system.dept.db.SysDeptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author likanug
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LocalCacheService {

    private final SysConfigService configService;

    private final SysDeptService deptService;

    public final AbstractCaffeineCacheTemplate<String> configCache = new AbstractCaffeineCacheTemplate<String>() {
        @Override
        public String getObjectFromDb(Object id) {
            return configService.getConfigValueByKey(id.toString());
        }
    };

    public final AbstractCaffeineCacheTemplate<SysDeptEntity> deptCache = new AbstractCaffeineCacheTemplate<SysDeptEntity>() {
        @Override
        public SysDeptEntity getObjectFromDb(Object id) {
            return deptService.getById(Long.parseLong(id.toString()));
        }
    };


}
