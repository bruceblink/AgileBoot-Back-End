package app.keystone.admin.controller.system;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import app.keystone.admin.KeystoneAdminApplication;
import app.keystone.domain.system.role.RoleApplicationService;
import app.keystone.domain.system.role.command.UpdateRoleCommand;
import app.keystone.infrastructure.user.web.DataScopeEnum;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = KeystoneAdminApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.sql.init.mode=never"
})
class SysRolePreAuthorizeIntegrationTest {

    @Autowired
    private SysRoleController controller;

    @MockitoBean
    private RoleApplicationService roleApplicationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void editShouldDenyUserWithoutRoleEditPermission() {
        setLoginUserPermissions(Set.of("system:role:list"));

        assertThatThrownBy(() -> controller.edit(validCommand()))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(roleApplicationService);
    }

    @Test
    void editShouldAllowUserWithRoleEditPermission() {
        setLoginUserPermissions(Set.of("system:role:edit"));

        controller.edit(validCommand());

        verify(roleApplicationService).updateRole(any(UpdateRoleCommand.class));
    }

    @Test
    void editShouldAllowAdminWildcardPermission() {
        setLoginUserPermissions(RoleInfo.ADMIN_PERMISSIONS);

        controller.edit(validCommand());

        verify(roleApplicationService).updateRole(any(UpdateRoleCommand.class));
    }

    private void setLoginUserPermissions(Set<String> permissions) {
        RoleInfo roleInfo = new RoleInfo(2L, "tester", DataScopeEnum.ALL, Set.of(), permissions, Set.of());
        SystemLoginUser loginUser = new SystemLoginUser(2L, false, "tester", null, roleInfo, 1L);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private UpdateRoleCommand validCommand() {
        UpdateRoleCommand command = new UpdateRoleCommand();
        command.setRoleId(2L);
        command.setRoleName("普通角色");
        command.setRoleKey("common");
        command.setRoleSort(1);
        command.setStatus(1);
        command.setMenuIds(List.of(1L));
        return command;
    }
}
