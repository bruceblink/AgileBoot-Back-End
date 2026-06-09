package app.keystone.admin.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.system.serviceclient.ServiceClientApplicationService;
import app.keystone.domain.system.serviceclient.command.RegisterServiceClientCommand;
import app.keystone.domain.system.serviceclient.command.UpdateServiceClientCommand;
import app.keystone.domain.system.serviceclient.dto.ServiceClientDTO;
import app.keystone.domain.system.serviceclient.query.ServiceClientQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SysServiceClientControllerTest {

    private ServiceClientApplicationService serviceClientApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        serviceClientApplicationService = Mockito.mock(ServiceClientApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SysServiceClientController(serviceClientApplicationService))
            .build();
    }

    @Test
    void listShouldReturnServiceClients() throws Exception {
        ServiceClientDTO serviceClient = new ServiceClientDTO();
        serviceClient.setServiceId("order-svc");
        serviceClient.setName("Order Service");
        when(serviceClientApplicationService.list(any(ServiceClientQuery.class)))
            .thenReturn(new PageDTO<>(List.of(serviceClient), 1L));

        mockMvc.perform(get("/system/service-clients")
                .param("name", "order")
                .param("pageNum", "1")
                .param("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.rows[0].serviceId").value("order-svc"));

        verify(serviceClientApplicationService).list(any(ServiceClientQuery.class));
    }

    @Test
    void detailShouldReturnServiceClient() throws Exception {
        ServiceClientDTO serviceClient = new ServiceClientDTO();
        serviceClient.setServiceId("order-svc");
        serviceClient.setName("Order Service");
        when(serviceClientApplicationService.detail("order-svc")).thenReturn(serviceClient);

        mockMvc.perform(get("/system/service-clients/order-svc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.serviceId").value("order-svc"));

        verify(serviceClientApplicationService).detail("order-svc");
    }

    @Test
    void addShouldRegisterServiceClient() throws Exception {
        when(serviceClientApplicationService.register(any(RegisterServiceClientCommand.class)))
            .thenReturn(new ServiceClientDTO("order-svc", "Service registered successfully"));

        mockMvc.perform(post("/system/service-clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "serviceId": "order-svc",
                      "serviceSecret": "secret",
                      "name": "Order Service",
                      "description": "Order domain API",
                      "allowedScopes": ["read", "write"],
                      "allowedAudiences": ["inventory-svc"],
                      "integrationType": "internal",
                      "introspectionAllowed": true,
                      "tokenTtlSeconds": 3600,
                      "owner": "Platform Team",
                      "contact": "platform@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.serviceId").value("order-svc"));

        verify(serviceClientApplicationService).register(any(RegisterServiceClientCommand.class));
    }

    @Test
    void editShouldUpdateServiceClient() throws Exception {
        when(serviceClientApplicationService.update(any(String.class), any(UpdateServiceClientCommand.class)))
            .thenReturn(new ServiceClientDTO("order-svc", "Service updated successfully"));

        mockMvc.perform(put("/system/service-clients/order-svc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Order Service",
                      "description": "Order domain API",
                      "allowedScopes": ["read", "write"],
                      "allowedAudiences": ["inventory-svc"],
                      "active": true,
                      "integrationType": "internal",
                      "introspectionAllowed": true,
                      "tokenTtlSeconds": 3600,
                      "owner": "Platform Team",
                      "contact": "platform@example.com"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.serviceId").value("order-svc"));

        verify(serviceClientApplicationService).update(any(String.class), any(UpdateServiceClientCommand.class));
    }
}
