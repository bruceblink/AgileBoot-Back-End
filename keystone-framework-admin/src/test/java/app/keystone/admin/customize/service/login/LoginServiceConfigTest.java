package app.keystone.admin.customize.service.login;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.admin.customize.service.login.dto.ConfigDTO;
import app.keystone.admin.customize.service.login.keylo.KeyloCredentialVerifier;
import app.keystone.admin.customize.service.login.keylo.KeyloLoginUserResolver;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.enums.common.ConfigKeyEnum;
import app.keystone.common.enums.dictionary.DictionaryData;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.domain.system.dept.db.SysDeptService;
import app.keystone.domain.system.dict.DictApplicationService;
import app.keystone.domain.system.user.db.SysUserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

class LoginServiceConfigTest {

    @Test
    void getConfig_shouldLoadDictionaryFromDatabaseService() {
        SysConfigService configService = mock(SysConfigService.class);
        LocalCacheService localCache = new LocalCacheService(configService, mock(SysDeptService.class));
        DictApplicationService dictApplicationService = mock(DictApplicationService.class);
        LoginService loginService = new LoginService(
            mock(TokenService.class),
            mock(KeystoneRsaKeyService.class),
            mock(RedisCacheService.class),
            localCache,
            mock(AuthenticationManager.class),
            mock(SysUserService.class),
            dictApplicationService,
            mock(KeyloTokenVerifier.class),
            mock(KeyloCredentialVerifier.class),
            mock(KeyloProperties.class),
            mock(KeyloLoginUserResolver.class),
            mock(AsyncTaskFactory.class)
        );
        Map<String, List<DictionaryData>> dictionary = new LinkedHashMap<>();
        dictionary.put("common.status", List.of(new DictionaryData("正常", 1, "")));
        when(configService.getConfigValueByKey(ConfigKeyEnum.CAPTCHA.getValue())).thenReturn("false");
        when(dictApplicationService.getDictionaryDataMap()).thenReturn(dictionary);

        ConfigDTO config = loginService.getConfig();

        assertFalse(config.getIsCaptchaOn());
        assertSame(dictionary, config.getDictionary());
        verify(dictApplicationService).getDictionaryDataMap();
    }
}
