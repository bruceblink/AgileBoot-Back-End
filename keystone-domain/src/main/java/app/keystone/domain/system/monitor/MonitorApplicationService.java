package app.keystone.domain.system.monitor;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Internal;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.monitor.dto.OnlineUserDTO;
import app.keystone.domain.system.monitor.dto.RedisCacheInfoDTO;
import app.keystone.domain.system.monitor.dto.RedisCacheInfoDTO.CommandStatusDTO;
import app.keystone.domain.system.monitor.dto.ServerInfo;
import app.keystone.infrastructure.cache.redis.CacheKeyEnum;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author valarchie
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorApplicationService {

    private final RedisTemplate<String, ?> redisTemplate;

    private final RedisCacheService redisCacheService;

    public RedisCacheInfoDTO getRedisCacheInfo() {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) RedisServerCommands::info);
        Properties commandStats = (Properties) redisTemplate.execute(
            (RedisCallback<Object>) connection -> connection.serverCommands().info("commandstats"));
        Long dbSize = redisTemplate.execute(RedisServerCommands::dbSize);

        if (commandStats == null || info == null) {
            throw new ApiException(Internal.INTERNAL_ERROR, "获取Redis监控信息失败。");
        }

        RedisCacheInfoDTO cacheInfo = new RedisCacheInfoDTO();

        cacheInfo.setInfo(info);
        cacheInfo.setDbSize(dbSize);
        cacheInfo.setCommandStats(new ArrayList<>());

        commandStats.stringPropertyNames().forEach(key -> {
            String property = commandStats.getProperty(key);

            CommandStatusDTO commonStatus = new CommandStatusDTO();
            commonStatus.setName(StringUtils.removeStart(key, "cmdstat_"));
            commonStatus.setValue(extractCalls(property));

            cacheInfo.getCommandStats().add(commonStatus);
        });

        return cacheInfo;
    }

    public List<OnlineUserDTO> getOnlineUserList(String username, String ipAddress) {
        Collection<String> keys = redisTemplate.keys(CacheKeyEnum.LOGIN_USER_KEY.key() + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        Stream<OnlineUserDTO> onlineUserStream = keys.stream()
            .map(this::getOnlineUserByRedisKey)
            .filter(Objects::nonNull)
            .map(OnlineUserDTO::new);

        List<OnlineUserDTO> filteredOnlineUsers = onlineUserStream
            .filter(o ->
                StringUtils.isEmpty(username) || username.equals(o.getUsername())
            ).filter( o ->
                StringUtils.isEmpty(ipAddress) || ipAddress.equals(o.getIpAddress())
            ).collect(Collectors.toList());

        Collections.reverse(filteredOnlineUsers);
        return filteredOnlineUsers;
    }

    private SystemLoginUser getOnlineUserByRedisKey(String redisKey) {
        try {
            return redisCacheService.loginUserCache.getObjectOnlyInRedisById(getLoginUserTokenId(redisKey));
        } catch (RuntimeException e) {
            log.warn("Failed to load online user from redis key: {}", redisKey, e);
            return null;
        }
    }

    private String getLoginUserTokenId(String redisKey) {
        return StringUtils.removeStart(redisKey, CacheKeyEnum.LOGIN_USER_KEY.key());
    }

    public ServerInfo getServerInfo() {
        return ServerInfo.fillInfo();
    }

    private String extractCalls(String property) {
        if (StringUtils.isEmpty(property)) {
            return null;
        }

        String marker = "calls=";
        int start = property.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();

        int end = property.indexOf(",usec", start);
        if (end < 0) {
            return null;
        }

        return property.substring(start, end);
    }


}
