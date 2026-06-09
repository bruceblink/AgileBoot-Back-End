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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 缓存监控
 *
 * @author likanug
 */
@Tag(name = "监控API", description = "监控相关信息")
@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitorController extends BaseController {

    private final MonitorApplicationService monitorApplicationService;

    private final TokenService tokenService;

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
    public ResponseDTO<Void> logoutOnlineUser(@PathVariable String tokenId) {
        tokenService.removeLoginUser(tokenId);
        return ResponseDTO.ok();
    }


}
