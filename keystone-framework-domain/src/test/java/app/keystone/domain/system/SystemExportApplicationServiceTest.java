package app.keystone.domain.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.enums.common.OperationStatusEnum;
import app.keystone.common.enums.common.OperatorTypeEnum;
import app.keystone.common.enums.common.RequestMethodEnum;
import app.keystone.domain.system.config.ConfigApplicationService;
import app.keystone.domain.system.config.db.SysConfigEntity;
import app.keystone.domain.system.config.db.SysConfigService;
import app.keystone.domain.system.config.dto.ConfigDTO;
import app.keystone.domain.system.config.model.ConfigModelFactory;
import app.keystone.domain.system.config.query.ConfigQuery;
import app.keystone.domain.system.log.LogApplicationService;
import app.keystone.domain.system.log.db.SysLoginInfoEntity;
import app.keystone.domain.system.log.db.SysLoginInfoService;
import app.keystone.domain.system.log.db.SysOperationLogEntity;
import app.keystone.domain.system.log.db.SysOperationLogService;
import app.keystone.domain.system.log.dto.LoginLogDTO;
import app.keystone.domain.system.log.dto.OperationLogDTO;
import app.keystone.domain.system.log.query.LoginLogQuery;
import app.keystone.domain.system.log.query.OperationLogQuery;
import app.keystone.domain.system.menu.db.SysMenuService;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.RoleApplicationService;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.role.dto.RoleDTO;
import app.keystone.domain.system.role.model.RoleModelFactory;
import app.keystone.domain.system.role.query.RoleQuery;
import app.keystone.domain.system.user.UserApplicationService;
import app.keystone.domain.system.user.db.SearchUserDO;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.domain.system.user.dto.UserDTO;
import app.keystone.domain.system.user.model.UserModelFactory;
import app.keystone.domain.system.user.query.SearchUserQuery;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class SystemExportApplicationServiceTest {

    @Test
    void exportUsersShouldUseUnpagedQuery() {
        SysUserService userService = mock(SysUserService.class);
        UserApplicationService service = new UserApplicationService(userService, mock(SysRoleService.class),
            mock(SysPostService.class), mock(UserModelFactory.class));
        SearchUserDO user = new SearchUserDO();
        user.setUserId(1001L);
        user.setUsername("export-user");
        when(userService.listUsersByQuery(any())).thenReturn(List.of(user));

        List<UserDTO> result = service.exportUsers(new SearchUserQuery<>());

        assertEquals(1, result.size());
        assertEquals("export-user", result.get(0).getUsername());
        verify(userService).listUsersByQuery(any());
        verify(userService, never()).getUserList(any());
    }

    @Test
    void exportRolesShouldUseUnpagedQuery() {
        SysRoleService roleService = mock(SysRoleService.class);
        RoleApplicationService service = new RoleApplicationService(mock(RoleModelFactory.class),
            mock(UserModelFactory.class), roleService, mock(SysUserService.class), mock(SysMenuService.class));
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleId(2L);
        role.setRoleName("operator");
        when(roleService.list(anyWrapper())).thenReturn(List.of(role));

        List<RoleDTO> result = service.exportRoles(new RoleQuery());

        assertEquals(1, result.size());
        assertEquals("operator", result.get(0).getRoleName());
        verify(roleService).list(anyWrapper());
        verify(roleService, never()).page(any(), any());
    }

    @Test
    void exportConfigsShouldUseUnpagedQuery() {
        SysConfigService configService = mock(SysConfigService.class);
        ConfigApplicationService service = new ConfigApplicationService(mock(ConfigModelFactory.class), configService);
        SysConfigEntity config = new SysConfigEntity();
        config.setConfigId(5);
        config.setConfigName("site name");
        when(configService.list(anyWrapper())).thenReturn(List.of(config));

        List<ConfigDTO> result = service.exportConfigs(new ConfigQuery());

        assertEquals(1, result.size());
        assertEquals("site name", result.get(0).getConfigName());
        verify(configService).list(anyWrapper());
        verify(configService, never()).page(any(), any());
    }

    @Test
    void exportLoginInfosShouldUseUnpagedQuery() {
        SysLoginInfoService loginInfoService = mock(SysLoginInfoService.class);
        LogApplicationService service = new LogApplicationService(loginInfoService, mock(SysOperationLogService.class));
        SysLoginInfoEntity log = new SysLoginInfoEntity();
        log.setInfoId(3L);
        log.setUsername("login-user");
        log.setStatus(LoginStatusEnum.LOGIN_SUCCESS.getValue());
        when(loginInfoService.list(anyWrapper())).thenReturn(List.of(log));

        List<LoginLogDTO> result = service.exportLoginInfos(new LoginLogQuery());

        assertEquals(1, result.size());
        assertEquals("login-user", result.get(0).getUsername());
        verify(loginInfoService).list(anyWrapper());
        verify(loginInfoService, never()).page(any(), any());
    }

    @Test
    void loginInfoQueryShouldDefaultOrderByLoginTimeDesc() {
        QueryWrapper<SysLoginInfoEntity> wrapper = new LoginLogQuery().toQueryWrapper();

        assertEquals("ORDER BY login_time DESC,info_id DESC", wrapper.getSqlSegment().trim());
    }

    @Test
    void loginInfoQueryShouldKeepStableOrderWhenLoginTimeSortProvided() {
        LoginLogQuery query = new LoginLogQuery();
        query.setOrderColumn("loginTime");
        query.setOrderDirection("descending");

        QueryWrapper<SysLoginInfoEntity> wrapper = query.toQueryWrapper();

        assertEquals("ORDER BY login_time DESC,info_id DESC", wrapper.getSqlSegment().trim());
    }

    @Test
    void exportOperationLogsShouldUseUnpagedQuery() {
        SysOperationLogService operationLogService = mock(SysOperationLogService.class);
        LogApplicationService service = new LogApplicationService(mock(SysLoginInfoService.class),
            operationLogService);
        SysOperationLogEntity log = new SysOperationLogEntity();
        log.setOperationId(4L);
        log.setUsername("operation-user");
        log.setBusinessType(BusinessTypeEnum.EXPORT.getValue());
        log.setRequestMethod(RequestMethodEnum.GET.getValue());
        log.setOperatorType(OperatorTypeEnum.WEB.getValue());
        log.setStatus(OperationStatusEnum.SUCCESS.getValue());
        when(operationLogService.list(anyWrapper())).thenReturn(List.of(log));

        List<OperationLogDTO> result = service.exportOperationLogs(new OperationLogQuery());

        assertEquals(1, result.size());
        assertEquals("operation-user", result.get(0).getUsername());
        verify(operationLogService).list(anyWrapper());
        verify(operationLogService, never()).page(any(), any());
    }

    @Test
    void operationLogQueryShouldDefaultOrderByOperationTimeDesc() {
        QueryWrapper<SysOperationLogEntity> wrapper = new OperationLogQuery().toQueryWrapper();

        assertEquals("ORDER BY operation_time DESC,operation_id DESC", wrapper.getSqlSegment().trim());
    }

    @Test
    void operationLogQueryShouldKeepStableOrderWhenOperationTimeSortProvided() {
        OperationLogQuery query = new OperationLogQuery();
        query.setOrderColumn("operationTime");
        query.setOrderDirection("descending");

        QueryWrapper<SysOperationLogEntity> wrapper = query.toQueryWrapper();

        assertEquals("ORDER BY operation_time DESC,operation_id DESC", wrapper.getSqlSegment().trim());
    }

    private static <T> Wrapper<T> anyWrapper() {
        return any();
    }
}
