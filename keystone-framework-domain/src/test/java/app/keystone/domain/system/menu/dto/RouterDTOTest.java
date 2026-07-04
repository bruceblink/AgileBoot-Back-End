package app.keystone.domain.system.menu.dto;

import static org.assertj.core.api.Assertions.assertThat;

import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.menu.db.SysMenuEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class RouterDTOTest {

    @Test
    void routerMetaShouldNotExposeButtonAuths() {
        SysMenuEntity menu = new SysMenuEntity();
        menu.setRouterName("SystemUser");
        menu.setPath("/system/user/index");
        menu.setPermission("system:user:list");
        menu.setMetaInfo("{\"title\":\"用户管理\"}");

        JsonNode json = JacksonUtil.from(JacksonUtil.to(new RouterDTO(menu)), JsonNode.class);

        assertThat(json.path("meta").path("title").asText()).isEqualTo("用户管理");
        assertThat(json.path("meta").has("auths")).isFalse();
    }
}
