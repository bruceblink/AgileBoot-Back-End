package app.keystone.admin.customize.service.login.keylo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Keylo access token identity.
 */
@Data
@AllArgsConstructor
public class KeyloTokenIdentity {

    /**
     * Keylo token sub claim, for example user:alice, client:admin, or service:sync.
     */
    private String keyloSubject;

    /**
     * Keylo token uid claim. Present for user tokens and absent for service/client tokens.
     */
    private String keyloUserId;

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private String tokenType;
}
