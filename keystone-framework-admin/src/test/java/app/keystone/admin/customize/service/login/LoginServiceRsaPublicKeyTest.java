package app.keystone.admin.customize.service.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.common.config.KeystoneConfig;
import app.keystone.domain.common.cache.LocalCacheService;
import app.keystone.domain.common.cache.RedisCacheService;
import app.keystone.domain.system.dict.DictApplicationService;
import app.keystone.domain.system.user.db.SysUserService;
import java.lang.reflect.Field;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;

class LoginServiceRsaPublicKeyTest {

    @AfterEach
    void tearDown() throws Exception {
        setKeystoneConfig(null);
    }

    @Test
    void getRsaPublicKey_shouldReturnPublicKeyDerivedFromConfiguredPrivateKey() throws Exception {
        KeyPair keyPair = generateKeyPair();
        LoginService loginService = createLoginService(keyPair);

        String publicKeyBase64 = loginService.getRsaPublicKey().getPublicKey();

        PublicKey publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
        assertEquals("RSA", publicKey.getAlgorithm());
        assertEquals(((RSAPublicKey) keyPair.getPublic()).getModulus(), ((RSAPublicKey) publicKey).getModulus());
        assertEquals(((RSAPublicKey) keyPair.getPublic()).getPublicExponent(),
            ((RSAPublicKey) publicKey).getPublicExponent());
    }

    @Test
    void decryptPassword_shouldDecryptPasswordEncryptedByReturnedPublicKey() throws Exception {
        KeyPair keyPair = generateKeyPair();
        LoginService loginService = createLoginService(keyPair);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
            new X509EncodedKeySpec(Base64.getDecoder().decode(loginService.getRsaPublicKey().getPublicKey())));

        String encryptedPassword = encrypt("plain-password", publicKey);

        assertEquals("plain-password", loginService.decryptPassword(encryptedPassword));
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private LoginService createLoginService(KeyPair keyPair) throws Exception {
        KeystoneConfig keystoneConfig = new KeystoneConfig();
        keystoneConfig.setRsaPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        setKeystoneConfig(keystoneConfig);

        return new LoginService(
            mock(TokenService.class),
            new KeystoneRsaKeyService(),
            mock(RedisCacheService.class),
            mock(LocalCacheService.class),
            mock(AuthenticationManager.class),
            mock(SysUserService.class),
            mock(DictApplicationService.class),
            mock(AsyncTaskFactory.class)
        );
    }

    private String encrypt(String text, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private void setKeystoneConfig(KeystoneConfig keystoneConfig) throws Exception {
        Field instanceField = KeystoneConfig.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, keystoneConfig);
    }
}
