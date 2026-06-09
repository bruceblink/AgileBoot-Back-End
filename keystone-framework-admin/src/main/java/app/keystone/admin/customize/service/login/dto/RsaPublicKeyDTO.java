package app.keystone.admin.customize.service.login.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * RSA public key used by clients to encrypt login passwords.
 */
@Data
@AllArgsConstructor
public class RsaPublicKeyDTO {

    private String publicKey;

}
