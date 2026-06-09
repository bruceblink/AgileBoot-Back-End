package app.keystone.domain.system.serviceclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.serviceclient.command.RegisterServiceClientCommand;
import app.keystone.domain.system.serviceclient.command.UpdateServiceClientCommand;
import app.keystone.domain.system.serviceclient.dto.ServiceClientDTO;
import app.keystone.domain.system.serviceclient.query.ServiceClientQuery;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningProperties;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ServiceClientApplicationServiceTest {

    @Test
    void registerShouldPostServiceClientToKeylo() {
        TestableServiceClientApplicationService service = new TestableServiceClientApplicationService(
            buildServiceClientKeyloProperties(), buildProvisioningProperties());
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200,
            "{\"service_id\":\"order-svc\",\"message\":\"Service registered successfully\"}"));

        ServiceClientDTO result = service.register(buildRegisterCommand());

        assertEquals("order-svc", result.getServiceId());
        assertEquals("http://keylo.local/v1/admin/token", service.requests.get(0).url());
        RecordedRequest registerRequest = service.requests.get(1);
        assertEquals("POST", registerRequest.method());
        assertEquals("http://keylo.local/v1/admin/services", registerRequest.url());
        assertEquals("Bearer admin-token", registerRequest.headers().get("Authorization"));
        assertEquals("order-svc", JacksonUtil.getAsString(registerRequest.jsonBody(), "service_id"));
        assertEquals("secret", JacksonUtil.getAsString(registerRequest.jsonBody(), "service_secret"));
        assertEquals("[\"read\",\"write\"]", JacksonUtil.getAsString(registerRequest.jsonBody(), "allowed_scopes"));
        assertEquals("[\"inventory-svc\"]",
            JacksonUtil.getAsString(registerRequest.jsonBody(), "allowed_audiences"));
    }

    @Test
    void listShouldQueryKeyloAndFilterInMemory() {
        TestableServiceClientApplicationService service = new TestableServiceClientApplicationService(
            buildServiceClientKeyloProperties(), buildProvisioningProperties());
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, """
            {
              "services": [
                {
                  "service_id": "order-svc",
                  "name": "Order Service",
                  "allowed_scopes": ["read"],
                  "allowed_audiences": ["inventory-svc"],
                  "active": true,
                  "integration_type": "internal",
                  "introspection_allowed": true,
                  "token_ttl_seconds": 3600,
                  "created_at": 1,
                  "updated_at": 2
                },
                {
                  "service_id": "billing-svc",
                  "name": "Billing Service",
                  "allowed_scopes": ["read"],
                  "allowed_audiences": ["order-svc"],
                  "active": false,
                  "integration_type": "job",
                  "introspection_allowed": true,
                  "created_at": 1,
                  "updated_at": 2
                }
              ]
            }
            """));
        ServiceClientQuery query = new ServiceClientQuery();
        query.setName("order");
        query.setPageNum(1);
        query.setPageSize(10);

        PageDTO<ServiceClientDTO> result = service.list(query);

        assertEquals(1L, result.getTotal());
        assertEquals("order-svc", result.getRows().get(0).getServiceId());
        RecordedRequest listRequest = service.requests.get(1);
        assertEquals("GET", listRequest.method());
        assertEquals("http://keylo.local/v1/admin/services", listRequest.url());
        assertEquals("Bearer admin-token", listRequest.headers().get("Authorization"));
    }

    @Test
    void detailShouldGetServiceClientFromKeylo() {
        TestableServiceClientApplicationService service = new TestableServiceClientApplicationService(
            buildServiceClientKeyloProperties(), buildProvisioningProperties());
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, """
            {
              "service_id": "order-svc",
              "name": "Order Service",
              "allowed_scopes": ["read"],
              "allowed_audiences": ["inventory-svc"],
              "active": true,
              "integration_type": "internal",
              "introspection_allowed": true,
              "created_at": 1,
              "updated_at": 2
            }
            """));

        ServiceClientDTO result = service.detail("order-svc");

        assertEquals("order-svc", result.getServiceId());
        assertEquals("GET", service.requests.get(1).method());
        assertEquals("http://keylo.local/v1/admin/services/order-svc", service.requests.get(1).url());
    }

    @Test
    void updateShouldPutServiceClientToKeylo() {
        TestableServiceClientApplicationService service = new TestableServiceClientApplicationService(
            buildServiceClientKeyloProperties(), buildProvisioningProperties());
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(200, "{\"message\":\"Service updated successfully\"}"));

        ServiceClientDTO result = service.update("order-svc", buildUpdateCommand());

        assertEquals("order-svc", result.getServiceId());
        RecordedRequest updateRequest = service.requests.get(1);
        assertEquals("PUT", updateRequest.method());
        assertEquals("http://keylo.local/v1/admin/services/order-svc", updateRequest.url());
        assertEquals("Order Service", JacksonUtil.getAsString(updateRequest.jsonBody(), "name"));
        assertEquals(Boolean.FALSE, JacksonUtil.fromMap(updateRequest.jsonBody()).get("active"));
    }

    @Test
    void registerShouldThrowServiceClientRegisterFailedWhenKeyloRejectsRequest() {
        TestableServiceClientApplicationService service = new TestableServiceClientApplicationService(
            buildServiceClientKeyloProperties(), buildProvisioningProperties());
        service.enqueue(response(200, "{\"access_token\":\"admin-token\"}"));
        service.enqueue(response(409,
            "{\"error\":\"conflict\",\"message\":\"Service client 'order-svc' already exists\"}"));

        ApiException exception = assertThrows(ApiException.class, () -> service.register(buildRegisterCommand()));

        assertEquals(ErrorCode.Business.KEYLO_SERVICE_CLIENT_REGISTER_FAILED, exception.getErrorCode());
    }

    @Test
    void registerShouldThrowDisabledWhenKeyloDisabled() {
        ServiceClientKeyloProperties keyloProperties = buildServiceClientKeyloProperties();
        keyloProperties.setEnabled(false);
        ServiceClientApplicationService service = new TestableServiceClientApplicationService(
            keyloProperties, buildProvisioningProperties());

        ApiException exception = assertThrows(ApiException.class, () -> service.register(buildRegisterCommand()));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_DISABLED, exception.getErrorCode());
    }

    private RegisterServiceClientCommand buildRegisterCommand() {
        RegisterServiceClientCommand command = new RegisterServiceClientCommand();
        command.setServiceId("order-svc");
        command.setServiceSecret("secret");
        fillEditableFields(command);
        return command;
    }

    private UpdateServiceClientCommand buildUpdateCommand() {
        UpdateServiceClientCommand command = new UpdateServiceClientCommand();
        fillEditableFields(command);
        command.setActive(false);
        return command;
    }

    private void fillEditableFields(UpdateServiceClientCommand command) {
        command.setName("Order Service");
        command.setDescription("Order domain API");
        command.setAllowedScopes(List.of("write", "read", "read"));
        command.setAllowedAudiences(List.of("inventory-svc"));
        command.setIntegrationType("internal");
        command.setIntrospectionAllowed(true);
        command.setTokenTtlSeconds(3600L);
        command.setOwner("Platform Team");
        command.setContact("platform@example.com");
    }

    private ServiceClientKeyloProperties buildServiceClientKeyloProperties() {
        ServiceClientKeyloProperties properties = new ServiceClientKeyloProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://keylo.local");
        return properties;
    }

    private KeyloUserProvisioningProperties buildProvisioningProperties() {
        KeyloUserProvisioningProperties properties = new KeyloUserProvisioningProperties();
        properties.setAdminTokenUrl("http://keylo.local/v1/admin/token");
        properties.setAdminClientId("cli-admin-root");
        properties.setAdminClientSecret("strong-secret");
        properties.setAuthHeaderName("Authorization");
        properties.setTimeoutMillis(10000);
        return properties;
    }

    private HttpResponse<String> response(int statusCode, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static class TestableServiceClientApplicationService extends ServiceClientApplicationService {
        private final Deque<HttpResponse<String>> responses = new ArrayDeque<>();
        private final List<RecordedRequest> requests = new ArrayList<>();

        TestableServiceClientApplicationService(ServiceClientKeyloProperties keyloProperties,
            KeyloUserProvisioningProperties provisioningProperties) {
            super(keyloProperties, provisioningProperties);
        }

        void enqueue(HttpResponse<String> response) {
            responses.addLast(response);
        }

        @Override
        protected HttpResponse<String> sendJson(String method, String url, String jsonBody, Map<String, String> headers,
            int timeoutMillis) {
            requests.add(new RecordedRequest(method, url, jsonBody, headers));
            HttpResponse<String> response = responses.pollFirst();
            if (response == null) {
                throw new IllegalStateException("No mocked response for " + method + " " + url);
            }
            return response;
        }
    }

    private record RecordedRequest(String method, String url, String jsonBody, Map<String, String> headers) {
    }
}
