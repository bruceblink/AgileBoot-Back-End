package app.keystone.admin.customize.service.login;

import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.admin.customize.service.login.command.KeyloLoginCommand;
import app.keystone.admin.customize.service.login.command.LoginCommand;
import app.keystone.admin.customize.service.login.command.RefreshTokenCommand;
import app.keystone.admin.customize.service.login.dto.CaptchaDTO;
import app.keystone.admin.customize.service.login.dto.ConfigDTO;
import app.keystone.admin.customize.service.login.dto.RsaPublicKeyDTO;
import app.keystone.admin.customize.service.login.TokenService.IssuedToken;
import app.keystone.admin.customize.service.login.keylo.KeyloCredentialVerifier;
import app.keystone.admin.customize.service.login.keylo.KeyloLoginUserResolver;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenIdentity;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.constant.Constants.Captcha;
import app.keystone.common.enums.common.ConfigKeyEnum;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.i18n.MessageUtils;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.MapCache;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.thread.ThreadPoolManager;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.code.kaptcha.Producer;
import jakarta.annotation.Resource;
import java.awt.image.BufferedImage;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.StringUtils;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LoginService {

    private final TokenService tokenService;

    private final RedisCacheService redisCache;

    private final LocalCacheService localCache;

    private final AuthenticationManager authenticationManager;

    private final SysUserService userService;

    private final KeyloTokenVerifier keyloTokenVerifier;

    private final KeyloCredentialVerifier keyloCredentialVerifier;

    private final KeyloProperties keyloProperties;

    private final KeyloLoginUserResolver keyloLoginUserResolver;

    @Value("${keystone.auth.mode}")
    private String authMode;

    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    /**
     * 登录验证
     *
     * @param loginCommand 登录参数
     * @return 结果
     */
    public LoginResult login(LoginCommand loginCommand) {
        if (isCaptchaOn()) {
            validateCaptcha(loginCommand.getUsername(), loginCommand.getCaptchaCode(), loginCommand.getCaptchaCodeKey());
        }

        if (!"local".equalsIgnoreCase(authMode) && keyloProperties.isEnabled()) {
            return loginByKeyloCredential(loginCommand);
        }

        if ("keylo-only".equalsIgnoreCase(authMode)) {
            throw new ApiException(Business.LOGIN_KEYLO_DISABLED);
        }

        Authentication authentication;
        String decryptPassword = decryptPassword(loginCommand.getPassword());
        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginCommand.getUsername(), decryptPassword));
        } catch (BadCredentialsException e) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginCommand.getUsername(), LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.LOGIN_WRONG_USER_PASSWORD")));
            throw new ApiException(e, ErrorCode.Business.LOGIN_WRONG_USER_PASSWORD);
        } catch (Exception e) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginCommand.getUsername(), LoginStatusEnum.LOGIN_FAIL, e.getMessage()));
            throw new ApiException(e, Business.LOGIN_ERROR, e.getMessage());
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SystemLoginUser loginUser = (SystemLoginUser) authentication.getPrincipal();
        IssuedToken issuedToken = createTokenAndRecordLoginInfo(loginUser, isForceLogin(loginCommand));
        return new LoginResult(issuedToken.getToken(), issuedToken.getRefreshToken(), issuedToken.getExpiresIn(),
            issuedToken.getRefreshExpiresIn(), null, null, null, null);
    }

    public LoginResult keyloLogin(KeyloLoginCommand keyloLoginCommand) {
        if (!keyloProperties.isLegacyTokenLoginEnabled()) {
            throw new ApiException(Business.LOGIN_KEYLO_DISABLED);
        }
        if ("local".equalsIgnoreCase(authMode) || !keyloProperties.isEnabled()) {
            throw new ApiException(Business.LOGIN_KEYLO_DISABLED);
        }
        if (keyloLoginCommand == null || !StringUtils.hasText(keyloLoginCommand.getAccessToken())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "accessToken is required");
        }

        log.warn("Deprecated /login/keylo endpoint was used. Please migrate clients to /login.");
        KeyloTokenIdentity keyloIdentity = keyloTokenVerifier.verify(keyloLoginCommand.getAccessToken());
        return buildTokenByKeyloIdentity(keyloIdentity);
    }

    private LoginResult loginByKeyloCredential(LoginCommand loginCommand) {
        if (loginCommand == null || !StringUtils.hasText(loginCommand.getUsername()) || !StringUtils.hasText(loginCommand.getPassword())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "username and password are required");
        }

        String password = decryptPassword(loginCommand.getPassword());
        KeyloTokenIdentity keyloIdentity = keyloCredentialVerifier.verify(loginCommand.getUsername(), password);
        return buildTokenByKeyloIdentity(keyloIdentity);
    }

    private LoginResult buildTokenByKeyloIdentity(KeyloTokenIdentity keyloIdentity) {
        SystemLoginUser loginUser = buildLoginUserByKeyloIdentity(keyloIdentity);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        IssuedToken issuedToken = createTokenAndRecordLoginInfo(loginUser, false);
        return new LoginResult(issuedToken.getToken(), issuedToken.getRefreshToken(), issuedToken.getExpiresIn(),
            issuedToken.getRefreshExpiresIn(), keyloIdentity.getAccessToken(), keyloIdentity.getRefreshToken(),
            keyloIdentity.getExpiresIn(), keyloIdentity.getTokenType());
    }

    public SystemLoginUser buildLoginUserByKeyloIdentity(KeyloTokenIdentity keyloIdentity) {
        return keyloLoginUserResolver.resolve(keyloIdentity);
    }

    public LoginResult refreshToken(RefreshTokenCommand refreshTokenCommand) {
        if (refreshTokenCommand == null || !StringUtils.hasText(refreshTokenCommand.getRefreshToken())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "refreshToken is required");
        }
        IssuedToken issuedToken = tokenService.refreshAccessToken(refreshTokenCommand.getRefreshToken());
        return new LoginResult(issuedToken.getToken(), issuedToken.getRefreshToken(), issuedToken.getExpiresIn(),
            issuedToken.getRefreshExpiresIn(), null, null, null, null);
    }

    public void logoutRefreshToken(RefreshTokenCommand refreshTokenCommand) {
        if (refreshTokenCommand == null || !StringUtils.hasText(refreshTokenCommand.getRefreshToken())) {
            return;
        }
        tokenService.removeLoginUserByRefreshToken(refreshTokenCommand.getRefreshToken());
    }

    private IssuedToken createTokenAndRecordLoginInfo(SystemLoginUser loginUser, boolean forceLogin) {
        IssuedToken issuedToken = tokenService.createTokenAndPutUserInCache(loginUser, forceLogin);
        try {
            recordLoginInfo(loginUser);
            return issuedToken;
        } catch (RuntimeException e) {
            tokenService.removeLoginUser(loginUser);
            throw e;
        }
    }

    private boolean isForceLogin(LoginCommand loginCommand) {
        return loginCommand != null && Boolean.TRUE.equals(loginCommand.getForceLogin());
    }

    @Data
    @AllArgsConstructor
    public static class LoginResult {

        private String token;

        private String refreshToken;

        private Long expiresIn;

        private Long refreshExpiresIn;

        private String keyloAccessToken;

        private String keyloRefreshToken;

        private Long keyloExpiresIn;

        private String keyloTokenType;
    }

    /**
     * 获取验证码 data
     *
     * @return {@link ConfigDTO}
     */
    public ConfigDTO getConfig() {
        ConfigDTO configDTO = new ConfigDTO();

        boolean isCaptchaOn = isCaptchaOn();
        configDTO.setIsCaptchaOn(isCaptchaOn);
        configDTO.setDictionary(MapCache.dictionaryCache());
        return configDTO;
    }

    public RsaPublicKeyDTO getRsaPublicKey() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = generatePrivateKey(keyFactory);
            if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR, "Invalid RSA private key");
            }
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return new RsaPublicKeyDTO(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Internal.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * 获取验证码 data
     *
     * @return 验证码
     */
    public CaptchaDTO generateCaptchaImg() {
        CaptchaDTO captchaDTO = new CaptchaDTO();

        boolean isCaptchaOn = isCaptchaOn();
        captchaDTO.setIsCaptchaOn(isCaptchaOn);

        if (isCaptchaOn) {
            String expression;
            String answer = null;
            BufferedImage image = null;

            String captchaType = KeystoneConfig.getCaptchaType();
            if (Captcha.MATH_TYPE.equals(captchaType)) {
                String capText = captchaProducerMath.createText();
                String[] expressionAndAnswer = capText.split("@");
                expression = expressionAndAnswer[0];
                answer = expressionAndAnswer[1];
                image = captchaProducerMath.createImage(expression);
            }

            if (Captcha.CHAR_TYPE.equals(captchaType)) {
                expression = answer = captchaProducer.createText();
                image = captchaProducer.createImage(expression);
            }

            if (image == null) {
                throw new ApiException(ErrorCode.Internal.LOGIN_CAPTCHA_GENERATE_FAIL);
            }

            String imgKey = UUID.randomUUID().toString().replace("-", "");

            redisCache.captchaCache.set(imgKey, answer);
            FastByteArrayOutputStream os = new FastByteArrayOutputStream();
            try {
                ImageIO.write(image, "jpg", os);
            } catch (Exception e) {
                throw new ApiException(e, ErrorCode.Internal.LOGIN_CAPTCHA_GENERATE_FAIL);
            }

            captchaDTO.setCaptchaCodeKey(imgKey);
            captchaDTO.setCaptchaCodeImg(Base64.getEncoder().encodeToString(os.toByteArray()));

        }

        return captchaDTO;
    }


    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param captchaCode 验证码
     * @param captchaCodeKey 验证码对应的缓存key
     */
    public void validateCaptcha(String username, String captchaCode, String captchaCodeKey) {
        String captcha = redisCache.captchaCache.getObjectById(captchaCodeKey);
        redisCache.captchaCache.delete(captchaCodeKey);
        if (captcha == null) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(username, LoginStatusEnum.LOGIN_FAIL,
                ErrorCode.Business.LOGIN_CAPTCHA_CODE_EXPIRE.message()));
            throw new ApiException(ErrorCode.Business.LOGIN_CAPTCHA_CODE_EXPIRE);
        }
        if (!captchaCode.equalsIgnoreCase(captcha)) {
            ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(username, LoginStatusEnum.LOGIN_FAIL,
                ErrorCode.Business.LOGIN_CAPTCHA_CODE_WRONG.message()));
            throw new ApiException(ErrorCode.Business.LOGIN_CAPTCHA_CODE_WRONG);
        }
    }

    /**
     * 记录登录信息
     * @param loginUser 登录用户
     */
    public void recordLoginInfo(SystemLoginUser loginUser) {
        ThreadPoolManager.execute(AsyncTaskFactory.loginInfoTask(loginUser.getUsername(), LoginStatusEnum.LOGIN_SUCCESS,
            LoginStatusEnum.LOGIN_SUCCESS.description()));

        LambdaUpdateWrapper<SysUserEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUserEntity::getUserId, loginUser.getUserId())
            .set(SysUserEntity::getLoginIp, ServletHolderUtil.getRequest().getRemoteAddr())
            .set(SysUserEntity::getLoginDate, new Date());
        userService.update(updateWrapper);
        redisCache.userCache.delete(loginUser.getUserId());
    }

    public String decryptPassword(String originalPassword) {
        try {
            PrivateKey privateKey = generatePrivateKey(KeyFactory.getInstance("RSA"));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptBytes = cipher.doFinal(Base64.getDecoder().decode(originalPassword));
            return new String(decryptBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.LOGIN_ERROR, e.getMessage());
        }
    }

    private PrivateKey generatePrivateKey(KeyFactory keyFactory) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(KeystoneConfig.getRsaPrivateKey());
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
    }

    private boolean isCaptchaOn() {
        return Boolean.parseBoolean(String.valueOf(localCache.configCache.get(ConfigKeyEnum.CAPTCHA.getValue())));
    }
}
