package app.keystone.admin.customize.service.login.keylo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Keylo 鉴权配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "keystone.auth.keylo")
public class KeyloProperties {

    private boolean enabled = true;

    private String baseUrl;

    private boolean legacyTokenLoginEnabled = true;

    private String issuerUri;

    private List<String> trustedIssuers = new ArrayList<>();

    private String jwkSetUri;

    private String subjectClaim = "sub";

    private String userIdClaim = "uid";

    private Integer clockSkewSeconds = 60;

    private List<String> audiences = new ArrayList<>();

    private String credentialVerifyUrl;

    private String credentialAuthHeaderName = "Authorization";

    private String credentialAuthHeaderValue;

    private String credentialUsernameField = "username";

    private String credentialPasswordField = "password";
}
