package app.keystone.domain.system.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.monitor.dto.OnlineUserDTO;
import app.keystone.infrastructure.cache.redis.CacheKeyEnum;
import app.keystone.infrastructure.cache.redis.RedisCacheTemplate;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

@SuppressWarnings("unchecked")
class MonitorApplicationServiceTest {

    private final RedisTemplate<String, ?> redisTemplate = mock(RedisTemplate.class);
    private final RedisCacheService redisCacheService = mock(RedisCacheService.class);
    private final RedisCacheTemplate<SystemLoginUser> loginUserCache = mock(RedisCacheTemplate.class);
    private final MonitorApplicationService monitorApplicationService =
        new MonitorApplicationService(redisTemplate, redisCacheService);

    @Test
    void getOnlineUserList_shouldLoadUsersByTokenIdFromRedis() {
        redisCacheService.loginUserCache = loginUserCache;
        SystemLoginUser loginUser = loginUser("token-1", "admin");
        when(redisTemplate.keys(CacheKeyEnum.LOGIN_USER_KEY.key() + "*"))
            .thenReturn(Set.of(CacheKeyEnum.LOGIN_USER_KEY.key() + "token-1"));
        when(loginUserCache.getObjectOnlyInRedisById("token-1")).thenReturn(loginUser);

        List<OnlineUserDTO> onlineUsers = monitorApplicationService.getOnlineUserList(null, null);

        assertThat(onlineUsers).hasSize(1);
        assertThat(onlineUsers.get(0).getTokenId()).isEqualTo("token-1");
        assertThat(onlineUsers.get(0).getUsername()).isEqualTo("admin");
        assertThat(onlineUsers.get(0).getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void getOnlineUserList_shouldSkipBrokenCacheEntry() {
        redisCacheService.loginUserCache = loginUserCache;
        SystemLoginUser loginUser = loginUser("token-1", "admin");
        when(redisTemplate.keys(CacheKeyEnum.LOGIN_USER_KEY.key() + "*"))
            .thenReturn(Set.of(
                CacheKeyEnum.LOGIN_USER_KEY.key() + "token-1",
                CacheKeyEnum.LOGIN_USER_KEY.key() + "broken-token"
            ));
        when(loginUserCache.getObjectOnlyInRedisById("token-1")).thenReturn(loginUser);
        when(loginUserCache.getObjectOnlyInRedisById("broken-token")).thenThrow(new IllegalStateException("bad data"));

        List<OnlineUserDTO> onlineUsers = monitorApplicationService.getOnlineUserList(null, null);

        assertThat(onlineUsers)
            .extracting(OnlineUserDTO::getTokenId)
            .containsExactly("token-1");
    }

    @Test
    void getOnlineUserList_shouldFilterByUsernameAndIpAddress() {
        redisCacheService.loginUserCache = loginUserCache;
        SystemLoginUser admin = loginUser("token-1", "admin");
        SystemLoginUser tester = loginUser("token-2", "tester");
        tester.getLoginInfo().setIpAddress("10.0.0.2");
        when(redisTemplate.keys(CacheKeyEnum.LOGIN_USER_KEY.key() + "*"))
            .thenReturn(Set.of(
                CacheKeyEnum.LOGIN_USER_KEY.key() + "token-1",
                CacheKeyEnum.LOGIN_USER_KEY.key() + "token-2"
            ));
        when(loginUserCache.getObjectOnlyInRedisById("token-1")).thenReturn(admin);
        when(loginUserCache.getObjectOnlyInRedisById("token-2")).thenReturn(tester);

        List<OnlineUserDTO> onlineUsers = monitorApplicationService.getOnlineUserList("tester", "10.0.0.2");

        assertThat(onlineUsers)
            .extracting(OnlineUserDTO::getTokenId)
            .containsExactly("token-2");
    }

    private SystemLoginUser loginUser(String tokenId, String username) {
        SystemLoginUser loginUser = new SystemLoginUser(1L, false, username, "pwd", RoleInfo.EMPTY_ROLE, null);
        loginUser.setCachedKey(tokenId);
        loginUser.getLoginInfo().setIpAddress("127.0.0.1");
        loginUser.getLoginInfo().setLocation("local");
        loginUser.getLoginInfo().setBrowser("Chrome");
        loginUser.getLoginInfo().setOperationSystem("Windows");
        loginUser.getLoginInfo().setLoginTime(1000L);
        return loginUser;
    }
}
