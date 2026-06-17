package app.keystone.admin.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.system.dict.DictApplicationService;
import app.keystone.domain.system.dict.dto.DictDataDTO;
import app.keystone.domain.system.dict.dto.DictTypeDTO;
import app.keystone.domain.system.dict.query.DictTypeQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SysDictControllerTest {

    private DictApplicationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(DictApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SysDictController(service)).build();
    }

    @Test
    void listDictTypesShouldSupportFrontendListAlias() throws Exception {
        when(service.getDictTypeList(any(DictTypeQuery.class)))
            .thenReturn(new PageDTO<>(List.of(new DictTypeDTO(null)), 1L));

        mockMvc.perform(get("/system/dict/type/list")
                .queryParam("pageNum", "1")
                .queryParam("pageSize", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.rows.length()").value(1));

        verify(service).getDictTypeList(any(DictTypeQuery.class));
    }

    @Test
    void getDictDataByTypeShouldReturnDropdownData() throws Exception {
        when(service.getDictDataByType("sys_user_sex"))
            .thenReturn(List.of(new DictDataDTO(null)));

        mockMvc.perform(get("/system/dict/data/type/sys_user_sex"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.length()").value(1));

        verify(service).getDictDataByType("sys_user_sex");
    }

    @Test
    void refreshCacheShouldRefreshDictCaches() throws Exception {
        mockMvc.perform(delete("/system/dict/cache"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(service).refreshCaches();
    }
}
