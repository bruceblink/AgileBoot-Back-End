package app.keystone.domain.system.serviceclient;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.serviceclient.command.RegisterServiceClientCommand;
import app.keystone.domain.system.serviceclient.command.UpdateServiceClientCommand;
import app.keystone.domain.system.serviceclient.dto.ServiceClientDTO;
import app.keystone.domain.system.serviceclient.query.ServiceClientQuery;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningProperties;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceClientApplicationService {

    private static final String SERVICE_PATH = "/v1/admin/services";

    private final ServiceClientKeyloProperties keyloProperties;

    private final KeyloUserProvisioningProperties provisioningProperties;

    public PageDTO<ServiceClientDTO> list(ServiceClientQuery query) {
        validateConfig();

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendJson(
                "GET",
                serviceUrl(),
                null,
                Map.of(provisioningProperties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                provisioningProperties.getTimeoutMillis()
            );

            String responseBody = response.body();
            checkResponse(response, ErrorCode.Business.KEYLO_SERVICE_CLIENT_QUERY_FAILED);

            List<ServiceClientDTO> services = JacksonUtil.getAsList(responseBody, "services", ServiceClientDTO.class)
                .stream()
                .filter(service -> matches(query, service))
                .toList();

            int pageNum = Objects.requireNonNullElse(query == null ? null : query.getPageNum(), 1);
            int pageSize = Objects.requireNonNullElse(query == null ? null : query.getPageSize(), 10);
            int fromIndex = Math.min(Math.max(pageNum - 1, 0) * pageSize, services.size());
            int toIndex = Math.min(fromIndex + pageSize, services.size());
            return new PageDTO<>(services.subList(fromIndex, toIndex), (long) services.size());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.KEYLO_SERVICE_CLIENT_QUERY_FAILED, e.getMessage());
        }
    }

    public ServiceClientDTO detail(String serviceId) {
        validateConfig();
        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendJson(
                "GET",
                serviceDetailUrl(serviceId),
                null,
                Map.of(provisioningProperties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                provisioningProperties.getTimeoutMillis()
            );
            checkResponse(response, ErrorCode.Business.KEYLO_SERVICE_CLIENT_QUERY_FAILED);
            return JacksonUtil.from(response.body(), ServiceClientDTO.class);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.KEYLO_SERVICE_CLIENT_QUERY_FAILED, e.getMessage());
        }
    }

    public ServiceClientDTO register(RegisterServiceClientCommand command) {
        validateConfig();

        Map<String, Object> body = requestBody(command, true);

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendJson(
                "POST",
                serviceUrl(),
                JacksonUtil.to(body),
                Map.of(provisioningProperties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                provisioningProperties.getTimeoutMillis()
            );

            checkResponse(response, ErrorCode.Business.KEYLO_SERVICE_CLIENT_REGISTER_FAILED);
            return new ServiceClientDTO(
                StringUtils.defaultIfBlank(jsonField(response.body(), "service_id"), command.getServiceId()),
                StringUtils.defaultIfBlank(jsonField(response.body(), "message"), "Service registered successfully")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.KEYLO_SERVICE_CLIENT_REGISTER_FAILED, e.getMessage());
        }
    }

    public ServiceClientDTO update(String serviceId, UpdateServiceClientCommand command) {
        validateConfig();

        Map<String, Object> body = requestBody(command, false);

        try {
            String adminAccessToken = getAdminAccessToken();
            HttpResponse<String> response = sendJson(
                "PUT",
                serviceDetailUrl(serviceId),
                JacksonUtil.to(body),
                Map.of(provisioningProperties.getAuthHeaderName(), "Bearer " + adminAccessToken),
                provisioningProperties.getTimeoutMillis()
            );

            checkResponse(response, ErrorCode.Business.KEYLO_SERVICE_CLIENT_UPDATE_FAILED);
            return new ServiceClientDTO(
                serviceId,
                StringUtils.defaultIfBlank(jsonField(response.body(), "message"), "Service updated successfully")
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.KEYLO_SERVICE_CLIENT_UPDATE_FAILED, e.getMessage());
        }
    }

    private void validateConfig() {
        if (!keyloProperties.isEnabled()) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_DISABLED);
        }
        if (StringUtils.isBlank(keyloProperties.getBaseUrl())
            || StringUtils.isBlank(provisioningProperties.getAdminTokenUrl())
            || StringUtils.isBlank(provisioningProperties.getAdminClientId())
            || StringUtils.isBlank(provisioningProperties.getAdminClientSecret())) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_CONFIG_MISSING);
        }
    }

    private String getAdminAccessToken() {
        Map<String, Object> tokenBody = new HashMap<>();
        tokenBody.put("client_id", provisioningProperties.getAdminClientId());
        tokenBody.put("client_secret", provisioningProperties.getAdminClientSecret());

        HttpResponse<String> tokenResponse = sendJson(
            "POST",
            provisioningProperties.getAdminTokenUrl(),
            JacksonUtil.to(tokenBody),
            Map.of(),
            provisioningProperties.getTimeoutMillis()
        );

        if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
            throw new ApiException(ErrorCode.Business.KEYLO_SERVICE_CLIENT_REGISTER_FAILED,
                "admin token HTTP " + tokenResponse.statusCode());
        }

        String adminAccessToken = jsonField(tokenResponse.body(), "access_token");
        if (StringUtils.isBlank(adminAccessToken)) {
            throw new ApiException(ErrorCode.Business.KEYLO_SERVICE_CLIENT_REGISTER_FAILED,
                "admin access_token missing");
        }
        return adminAccessToken;
    }

    protected HttpResponse<String> sendJson(String method, String url, String jsonBody, Map<String, String> headers,
        int timeoutMillis) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json");
            if ("GET".equalsIgnoreCase(method)) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(StringUtils.defaultString(jsonBody)));
            }
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
            return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ApiException(e, ErrorCode.Business.KEYLO_SERVICE_CLIENT_QUERY_FAILED, e.getMessage());
        }
    }

    private void checkResponse(HttpResponse<String> response, ErrorCode.Business errorCode) {
        String responseBody = response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ApiException(errorCode, errorMessage(responseBody, "HTTP " + response.statusCode()));
        }

        String error = jsonField(responseBody, "error");
        if (StringUtils.isNotBlank(error)) {
            throw new ApiException(errorCode, errorMessage(responseBody, error));
        }
    }

    private Map<String, Object> requestBody(UpdateServiceClientCommand command, boolean includeSecret) {
        Map<String, Object> body = new HashMap<>();
        if (includeSecret) {
            RegisterServiceClientCommand registerCommand = (RegisterServiceClientCommand) command;
            body.put("service_id", registerCommand.getServiceId());
            body.put("service_secret", registerCommand.getServiceSecret());
        }
        body.put("name", command.getName());
        body.put("description", command.getDescription());
        body.put("allowed_scopes", normalizeList(command.getAllowedScopes()));
        body.put("allowed_audiences", normalizeList(command.getAllowedAudiences()));
        if (!includeSecret) {
            body.put("active", command.getActive());
        }
        body.put("integration_type", StringUtils.defaultIfBlank(command.getIntegrationType(), "internal"));
        body.put("introspection_allowed", command.getIntrospectionAllowed());
        body.put("token_ttl_seconds", command.getTokenTtlSeconds());
        body.put("owner", command.getOwner());
        body.put("contact", command.getContact());
        return body;
    }

    private boolean matches(ServiceClientQuery query, ServiceClientDTO service) {
        if (query == null) {
            return true;
        }
        return contains(service.getServiceId(), query.getServiceId())
            && contains(service.getName(), query.getName())
            && equalsIfPresent(service.getActive(), query.getActive())
            && contains(service.getIntegrationType(), query.getIntegrationType());
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.isBlank(keyword) || StringUtils.containsIgnoreCase(value, keyword.trim());
    }

    private boolean equalsIfPresent(Boolean value, Boolean expected) {
        return expected == null || Objects.equals(value, expected);
    }

    private String serviceUrl() {
        return StringUtils.removeEnd(keyloProperties.getBaseUrl(), "/") + SERVICE_PATH;
    }

    private String serviceDetailUrl(String serviceId) {
        return serviceUrl() + "/" + URLEncoder.encode(serviceId, StandardCharsets.UTF_8);
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .map(StringUtils::trimToEmpty)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .sorted()
            .toList();
    }

    private String errorMessage(String responseBody, String fallback) {
        String message = jsonField(responseBody, "message");
        return StringUtils.defaultIfBlank(message, fallback);
    }

    private String jsonField(String responseBody, String fieldName) {
        try {
            return JacksonUtil.getAsString(responseBody, fieldName);
        } catch (Exception e) {
            return null;
        }
    }
}
