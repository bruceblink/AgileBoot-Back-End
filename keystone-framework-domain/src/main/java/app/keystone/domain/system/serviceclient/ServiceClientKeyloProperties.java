package app.keystone.domain.system.serviceclient;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "keystone.auth.keylo")
public class ServiceClientKeyloProperties {

    private boolean enabled = true;

    private String baseUrl;
}
