package app.keystone.infrastructure.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Rsa key生成
 * @author likanug
 */
public class RsaKeyPairGenerator {

    public static void main(String[] args) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            System.out.println(privateKeyBase64);
            System.out.println(publicKeyBase64);
        } catch (Exception e) {
            throw new IllegalStateException("Generate RSA key pair failed", e);
        }
    }


}
