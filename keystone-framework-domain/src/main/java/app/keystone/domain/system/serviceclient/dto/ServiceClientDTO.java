package app.keystone.domain.system.serviceclient.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceClientDTO {

    @JsonAlias("service_id")
    private String serviceId;

    private String name;

    private String description;

    @JsonAlias("allowed_scopes")
    private List<String> allowedScopes;

    @JsonAlias("allowed_audiences")
    private List<String> allowedAudiences;

    private Boolean active;

    @JsonAlias("integration_type")
    private String integrationType;

    @JsonAlias("introspection_allowed")
    private Boolean introspectionAllowed;

    @JsonAlias("token_ttl_seconds")
    private Long tokenTtlSeconds;

    private String owner;

    private String contact;

    @JsonAlias("created_at")
    private Long createdAt;

    @JsonAlias("updated_at")
    private Long updatedAt;

    private String message;

    public ServiceClientDTO(String serviceId, String message) {
        this.serviceId = serviceId;
        this.message = message;
    }
}
