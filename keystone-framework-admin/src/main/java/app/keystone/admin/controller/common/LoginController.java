package app.keystone.admin.controller.common;

import app.keystone.admin.customize.service.login.LoginService;
import app.keystone.admin.customize.service.login.LoginService.LoginResult;
import app.keystone.admin.customize.service.login.command.LoginCommand;
import app.keystone.admin.customize.service.login.command.RefreshTokenCommand;
import app.keystone.admin.customize.service.login.dto.CaptchaDTO;
import app.keystone.admin.customize.service.login.dto.ConfigDTO;
import app.keystone.admin.customize.service.login.dto.RsaPublicKeyDTO;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.common.dto.CurrentLoginUserDTO;
import app.keystone.domain.common.dto.TokenDTO;
import app.keystone.domain.system.menu.MenuApplicationService;
import app.keystone.domain.system.menu.dto.RouterDTO;
import app.keystone.domain.system.user.UserApplicationService;
import app.keystone.domain.system.user.command.AddUserCommand;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit.LimitType;
import app.keystone.infrastructure.annotations.ratelimit.RateLimitKey;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页
 *
 * @author likanug
 */
@Tag(name = "登录API", description = "登录相关接口")
@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    private final MenuApplicationService menuApplicationService;

    private final UserApplicationService userApplicationService;

    /**
     * 触发服务健康检查
     *
     * @return 默认的请求成功信息
     */
    @GetMapping("/health")
    public ResponseDTO<String> health() {
        log.info("health check running");
        return ResponseDTO.ok("is alive");
    }

    /**
     * 获取系统的内置配置
     *
     * @return 配置信息
     */
    @RateLimit(key = RateLimitKey.PREFIX, time = 10, maxCount = 5, limitType = RateLimit.LimitType.GLOBAL)
    @GetMapping("/getConfig")
    public ResponseDTO<ConfigDTO> getConfig() {
        ConfigDTO configDTO = loginService.getConfig();
        return ResponseDTO.ok(configDTO);
    }

    /**
     * 生成验证码
     */
    @Operation(summary = "验证码")
    @RateLimit(key = RateLimitKey.LOGIN_CAPTCHA_KEY, time = 10, maxCount = 10, limitType = LimitType.IP)
    @GetMapping("/captchaImage")
    public ResponseDTO<CaptchaDTO> getCaptchaImg() {
        CaptchaDTO captchaImg = loginService.generateCaptchaImg();
        return ResponseDTO.ok(captchaImg);
    }

    @Operation(summary = "获取 Keystone RSA 公钥", description = "客户端使用该公钥加密 /login 密码，并可用于验签 Keystone RS256 JWT")
    @RateLimit(key = RateLimitKey.LOGIN_RSA_PUBLIC_KEY, time = 60, maxCount = 60, limitType = LimitType.IP)
    @GetMapping("/login/rsa-public-key")
    public ResponseDTO<RsaPublicKeyDTO> getRsaPublicKey() {
        return ResponseDTO.ok(loginService.getRsaPublicKey());
    }

    /**
     * 登录方法
     *
     * @param loginCommand 登录信息
     * @return 结果
     */
    @Operation(summary = "登录", description = "使用 Keystone 本地账号密码认证")
    @PostMapping("/login")
    public ResponseDTO<TokenDTO> login(@RequestBody LoginCommand loginCommand) {
        LoginResult loginResult = loginService.login(loginCommand);
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        CurrentLoginUserDTO currentUserDTO = userApplicationService.getLoginUserInfo(loginUser);
        return ResponseDTO.ok(buildTokenDTO(loginResult, currentUserDTO));
    }

    @Operation(summary = "刷新 Keystone token", description = "使用 Keystone refresh token 换取新的 access token")
    @PostMapping("/refresh-token")
    public ResponseDTO<TokenDTO> refreshToken(@RequestBody RefreshTokenCommand refreshTokenCommand) {
        LoginResult loginResult = loginService.refreshToken(refreshTokenCommand);
        return ResponseDTO.ok(buildTokenDTO(loginResult, null));
    }

    @Operation(summary = "通过 refresh token 退出登录", description = "access token 已失效时用于释放 Keystone refresh 会话")
    @PostMapping("/logout-refresh-token")
    public ResponseDTO<Void> logoutRefreshToken(@RequestBody RefreshTokenCommand refreshTokenCommand) {
        loginService.logoutRefreshToken(refreshTokenCommand);
        return ResponseDTO.ok();
    }

    private TokenDTO buildTokenDTO(LoginResult loginResult, CurrentLoginUserDTO currentUserDTO) {
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setToken(loginResult.getToken());
        tokenDTO.setRefreshToken(loginResult.getRefreshToken());
        tokenDTO.setExpiresIn(loginResult.getExpiresIn());
        tokenDTO.setRefreshExpiresIn(loginResult.getRefreshExpiresIn());
        tokenDTO.setCurrentUser(currentUserDTO);
        return tokenDTO;
    }

    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/getLoginUserInfo")
    public ResponseDTO<CurrentLoginUserDTO> getLoginUserInfo() {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();

        CurrentLoginUserDTO currentUserDTO = userApplicationService.getLoginUserInfo(loginUser);

        return ResponseDTO.ok(currentUserDTO);
    }

    /**
     * 获取路由信息
     * @return 路由信息
     */
    @Operation(summary = "获取用户对应的菜单路由", description = "用于动态生成路由")
    @GetMapping("/getRouters")
    public ResponseDTO<List<RouterDTO>> getRouters() {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        List<RouterDTO> routerTree = menuApplicationService.getRouterTree(loginUser);
        return ResponseDTO.ok(routerTree);
    }

    @Operation(summary = "注册接口", description = "暂未实现")
    @PostMapping("/register")
    public ResponseDTO<Void> register(@RequestBody AddUserCommand command) {
        return ResponseDTO.fail(new ApiException(Business.COMMON_UNSUPPORTED_OPERATION));
    }
}
