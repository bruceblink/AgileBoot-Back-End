package app.keystone.admin.customize.service.login;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Provides Keystone's configured RSA key pair.
 */
@Component
public class KeystoneRsaKeyService {

    private volatile String cachedPrivateKeyBase64;

    private volatile RsaKeyPair cachedKeyPair;

    public PrivateKey getPrivateKey() {
        return loadKeyPair().privateKey();
    }

    public PublicKey getPublicKey() {
        return loadKeyPair().publicKey();
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(getPublicKey().getEncoded());
    }

    private RsaKeyPair loadKeyPair() {
        String privateKeyBase64 = KeystoneConfig.getRsaPrivateKey();
        if (!StringUtils.hasText(privateKeyBase64)) {
            throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR, "KEYSTONE_RSA_PRIVATE_KEY is not configured");
        }

        RsaKeyPair currentKeyPair = cachedKeyPair;
        if (currentKeyPair != null && privateKeyBase64.equals(cachedPrivateKeyBase64)) {
            return currentKeyPair;
        }

        synchronized (this) {
            currentKeyPair = cachedKeyPair;
            if (currentKeyPair != null && privateKeyBase64.equals(cachedPrivateKeyBase64)) {
                return currentKeyPair;
            }
            currentKeyPair = parseKeyPair(privateKeyBase64);
            cachedPrivateKeyBase64 = privateKeyBase64;
            cachedKeyPair = currentKeyPair;
            return currentKeyPair;
        }
    }

    private RsaKeyPair parseKeyPair(String privateKeyBase64) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            if (!(privateKey instanceof RSAPrivateCrtKey rsaPrivateKey)) {
                throw new ApiException(ErrorCode.Internal.INTERNAL_ERROR, "Invalid RSA private key");
            }

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
            return new RsaKeyPair(privateKey, publicKey);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Internal.INTERNAL_ERROR, e.getMessage());
        }
    }

    private record RsaKeyPair(PrivateKey privateKey, PublicKey publicKey) {
    }
}
