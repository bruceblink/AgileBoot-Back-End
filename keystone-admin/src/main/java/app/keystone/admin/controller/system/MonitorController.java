package app.keystone.admin.controller.system;

import app.keystone.admin.customize.service.login.TokenService;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.system.monitor.MonitorApplicationService;
import app.keystone.domain.system.monitor.dto.OnlineUserDTO;
import app.keystone.domain.system.monitor.dto.RedisCacheInfoDTO;
import app.keystone.domain.system.monitor.dto.ServerInfo;
import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.enums.common.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 缓存监控
 *
 * @author likanug
 */
@Tag(name = "监控API", description = "监控相关信息")
@RestController
@RequestMapping("/monitor")
@Validated
@RequiredArgsConstructor
public class MonitorController extends BaseController {

    private static final AtomicInteger NETWORK_STATUS_THREAD_ID = new AtomicInteger();

    private static final long NETWORK_STATUS_INTERVAL_SECONDS = 60L;

    private static final Duration NETWORK_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private static final Duration NETWORK_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private static final List<NetworkProbeTarget> NETWORK_PROBE_TARGETS = List.of(
        new NetworkProbeTarget("百度", URI.create("https://www.baidu.com")),
        new NetworkProbeTarget("Google", URI.create("https://www.google.com/generate_204")),
        new NetworkProbeTarget("阿里云", URI.create("https://www.aliyun.com"))
    );

    private final MonitorApplicationService monitorApplicationService;

    private final TokenService tokenService;

    private final ScheduledExecutorService networkStatusExecutor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("network-status-sse-" + NETWORK_STATUS_THREAD_ID.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient networkStatusHttpClient = HttpClient.newBuilder()
        .connectTimeout(NETWORK_CONNECT_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    @Operation(summary = "Redis信息")
    @PreAuthorize("@permission.has('monitor:cache:list')")
    @GetMapping("/cacheInfo")
    public ResponseDTO<RedisCacheInfoDTO> getRedisCacheInfo() {
        RedisCacheInfoDTO redisCacheInfo = monitorApplicationService.getRedisCacheInfo();
        return ResponseDTO.ok(redisCacheInfo);
    }


    @Operation(summary = "服务器信息")
    @PreAuthorize("@permission.has('monitor:server:list')")
    @GetMapping("/serverInfo")
    public ResponseDTO<ServerInfo> getServerInfo() {
        ServerInfo serverInfo = monitorApplicationService.getServerInfo();
        return ResponseDTO.ok(serverInfo);
    }

    @Operation(summary = "外网连接状态SSE")
    @PreAuthorize("@permission.has('monitor:server:list')")
    @GetMapping(value = "/networkStatus/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNetworkStatus() {
        SseEmitter emitter = new SseEmitter(0L);
        ScheduledFuture<?> future = networkStatusExecutor.scheduleWithFixedDelay(() -> sendNetworkStatus(emitter),
            0L, NETWORK_STATUS_INTERVAL_SECONDS, TimeUnit.SECONDS);
        Runnable cleanup = () -> future.cancel(true);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        return emitter;
    }

    /**
     * 获取在线用户列表
     *
     * @param ipAddress ip地址
     * @param username 用户名
     * @return 分页处理后的在线用户信息
     */
    @Operation(summary = "在线用户列表")
    @PreAuthorize("@permission.has('monitor:online:list')")
    @GetMapping("/onlineUsers")
    public ResponseDTO<PageDTO<OnlineUserDTO>> onlineUsers(String ipAddress, String username) {
        List<OnlineUserDTO> onlineUserList = monitorApplicationService.getOnlineUserList(username, ipAddress);
        return ResponseDTO.ok(new PageDTO<>(onlineUserList));
    }

    /**
     * 强退用户
     */
    @Operation(summary = "强退用户")
    @PreAuthorize("@permission.has('monitor:online:forceLogout')")
    @AccessLog(title = "在线用户", businessType = BusinessTypeEnum.FORCE_LOGOUT)
    @DeleteMapping("/onlineUser/{tokenId}")
    public ResponseDTO<Void> logoutOnlineUser(@PathVariable @NotBlank String tokenId) {
        tokenService.removeLoginUser(tokenId);
        return ResponseDTO.ok();
    }

    @PreDestroy
    public void destroy() {
        networkStatusExecutor.shutdownNow();
    }

    private void sendNetworkStatus(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                .name("network-status")
                .data(checkNetworkStatus(), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
        }
    }

    private NetworkStatusDTO checkNetworkStatus() {
        List<NetworkTargetStatusDTO> targets = NETWORK_PROBE_TARGETS.stream()
            .map(this::checkTarget)
            .toList();
        boolean online = targets.stream().anyMatch(NetworkTargetStatusDTO::connected);
        return new NetworkStatusDTO(online, online ? "ONLINE" : "OFFLINE", OffsetDateTime.now().toString(), targets);
    }

    private NetworkTargetStatusDTO checkTarget(NetworkProbeTarget target) {
        long startNanos = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(target.uri())
                .timeout(NETWORK_REQUEST_TIMEOUT)
                .header("User-Agent", "Keystone-Network-Status/1.0")
                .GET()
                .build();
            HttpResponse<Void> response = networkStatusHttpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            int statusCode = response.statusCode();
            boolean connected = statusCode >= 200 && statusCode < 400;
            return new NetworkTargetStatusDTO(target.name(), target.uri().toString(), connected, statusCode,
                latencyMillis, connected ? "连接正常" : "HTTP状态异常");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            return new NetworkTargetStatusDTO(target.name(), target.uri().toString(), false, null, latencyMillis,
                "检测已中断");
        } catch (Exception e) {
            long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            return new NetworkTargetStatusDTO(target.name(), target.uri().toString(), false, null, latencyMillis,
                e.getClass().getSimpleName());
        }
    }

    private record NetworkProbeTarget(String name, URI uri) {
    }

    public record NetworkStatusDTO(boolean online, String status, String checkedAt,
        List<NetworkTargetStatusDTO> targets) {
    }

    public record NetworkTargetStatusDTO(String name, String url, boolean connected, Integer statusCode,
        long latencyMillis, String message) {
    }

}
