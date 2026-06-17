package app.keystone.admin.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.system.config.ConfigApplicationService;
import app.keystone.domain.system.config.command.ConfigAddCommand;
import app.keystone.domain.system.config.dto.ConfigDTO;
import app.keystone.domain.system.config.query.ConfigQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SysConfigControllerTest {

    private ConfigApplicationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(ConfigApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SysConfigController(service)).build();
    }

    @Test
    void listShouldReturnConfigPage() throws Exception {
        when(service.getConfigList(any(ConfigQuery.class)))
            .thenReturn(new PageDTO<>(List.of(new ConfigDTO(null)), 1L));

        mockMvc.perform(get("/system/configs")
                .queryParam("pageNum", "1")
                .queryParam("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.rows.length()").value(1));

        verify(service).getConfigList(any(ConfigQuery.class));
    }

    @Test
    void addShouldCreateConfig() throws Exception {
        mockMvc.perform(post("/system/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "configName": "测试配置",
                      "configKey": "test.config",
                      "configOptions": ["enabled", "disabled"],
                      "configValue": "enabled",
                      "isAllowChange": 1,
                      "remark": "test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(service).addConfig(any(ConfigAddCommand.class));
    }

    @Test
    void refreshCacheShouldRefreshConfigCache() throws Exception {
        mockMvc.perform(delete("/system/configs/cache"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(service).refreshCaches();
    }
}
