package app.keystone.domain.system.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.command.AddUserCommand;
import app.keystone.domain.system.user.command.ResetPasswordCommand;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningResult;
import app.keystone.domain.system.user.keylo.KeyloUserProvisioningService;
import app.keystone.domain.system.user.model.UserModel;
import app.keystone.domain.system.user.model.UserModelFactory;
import org.junit.jupiter.api.Test;

class UserApplicationServiceAddUserTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysPostService postService = mock(SysPostService.class);
    private final UserModelFactory userModelFactory = mock(UserModelFactory.class);
    private final KeyloUserProvisioningService keyloUserProvisioningService = mock(KeyloUserProvisioningService.class);

    private final UserApplicationService userApplicationService = new UserApplicationService(
        userService,
        roleService,
        postService,
        userModelFactory,
        keyloUserProvisioningService
    );

    @Test
    void addUser_shouldSetExternalIdentity_whenProvisioningReturnsResult() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("new-user");
        command.setPassword("pwd");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.create()).thenReturn(userModel);
        when(keyloUserProvisioningService.provisionUser(command))
            .thenReturn(new KeyloUserProvisioningResult("sub-1001", "uid-1001"));

        userApplicationService.addUser(command);

        verify(userModel).insert();
        verify(userModel).setExternalSubject("sub-1001");
        verify(userModel).setExternalUserId("uid-1001");
        verify(userModel).updateById();
    }

    @Test
    void addUser_shouldNotUpdateExternalIdentity_whenProvisioningReturnsNull() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("new-user");
        command.setPassword("pwd");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.create()).thenReturn(userModel);
        when(keyloUserProvisioningService.provisionUser(command)).thenReturn(null);

        userApplicationService.addUser(command);

        verify(userModel).insert();
        verify(userModel, never()).setExternalSubject(org.mockito.ArgumentMatchers.anyString());
        verify(userModel, never()).setExternalUserId(org.mockito.ArgumentMatchers.anyString());
        verify(userModel, never()).updateById();
    }

    @Test
    void addUser_shouldThrowAndRollback_whenProvisioningFailed() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("new-user");
        command.setPassword("pwd");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.create()).thenReturn(userModel);
        when(keyloUserProvisioningService.provisionUser(command))
            .thenThrow(new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "timeout"));

        ApiException exception = assertThrows(ApiException.class, () -> userApplicationService.addUser(command));

        assertNotNull(exception);
        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
    }

    @Test
    void resetUserPassword_shouldNotUpdateLocalPassword_whenKeyloResetFailed() {
        ResetPasswordCommand command = new ResetPasswordCommand();
        command.setUserId(1001L);
        command.setPassword("NewPassword123!");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.loadById(1001L)).thenReturn(userModel);
        when(userModel.getExternalUserId()).thenReturn(null);
        doThrow(new ApiException(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, "Keylo user id missing"))
            .when(keyloUserProvisioningService).resetPassword(null, "NewPassword123!");

        ApiException exception = assertThrows(ApiException.class,
            () -> userApplicationService.resetUserPassword(command));

        assertEquals(ErrorCode.Business.LOGIN_KEYLO_PROVISION_FAILED, exception.getErrorCode());
        verify(userModel, never()).resetPassword("NewPassword123!");
        verify(userModel, never()).updateById();
    }
}
