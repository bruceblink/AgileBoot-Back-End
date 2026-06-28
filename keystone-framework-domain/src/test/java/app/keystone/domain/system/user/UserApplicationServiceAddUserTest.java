package app.keystone.domain.system.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import app.keystone.domain.system.user.command.UpdateUserCommand;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.domain.system.user.model.UserModel;
import app.keystone.domain.system.user.model.UserModelFactory;
import org.junit.jupiter.api.Test;

class UserApplicationServiceAddUserTest {

    private final SysUserService userService = mock(SysUserService.class);
    private final SysRoleService roleService = mock(SysRoleService.class);
    private final SysPostService postService = mock(SysPostService.class);
    private final UserModelFactory userModelFactory = mock(UserModelFactory.class);

    private final UserApplicationService userApplicationService = new UserApplicationService(
        userService,
        roleService,
        postService,
        userModelFactory
    );

    @Test
    void addUser_shouldInsertLocalUser() {
        AddUserCommand command = new AddUserCommand();
        command.setUsername("new-user");
        command.setPassword("pwd");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.create()).thenReturn(userModel);

        userApplicationService.addUser(command);

        verify(userModel).loadAddUserCommand(command);
        verify(userModel).checkUsernameIsUnique();
        verify(userModel).checkPhoneNumberIsUnique();
        verify(userModel).checkEmailIsUnique();
        verify(userModel).checkFieldRelatedEntityExist();
        verify(userModel).resetPassword("pwd");
        verify(userModel).insert();
        verify(userModel, never()).updateById();
    }

    @Test
    void updateUser_shouldRejectDuplicateUsername() {
        UpdateUserCommand command = new UpdateUserCommand();
        command.setUserId(1001L);
        command.setUsername("existing-user");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.loadById(1001L)).thenReturn(userModel);
        doThrow(new ApiException(ErrorCode.Business.USER_NAME_IS_NOT_UNIQUE))
            .when(userModel).checkUsernameIsUnique();

        ApiException exception = assertThrows(ApiException.class, () -> userApplicationService.updateUser(command));

        assertEquals(ErrorCode.Business.USER_NAME_IS_NOT_UNIQUE, exception.getErrorCode());
        verify(userModel).loadUpdateUserCommand(command);
        verify(userModel).checkUsernameIsUnique();
        verify(userModel, never()).updateById();
    }

    @Test
    void resetUserPassword_shouldUpdateLocalPassword() {
        ResetPasswordCommand command = new ResetPasswordCommand();
        command.setUserId(1001L);
        command.setPassword("NewPassword123!");

        UserModel userModel = mock(UserModel.class);
        when(userModelFactory.loadById(1001L)).thenReturn(userModel);

        userApplicationService.resetUserPassword(command);

        verify(userModel).resetPassword("NewPassword123!");
        verify(userModel).updateById();
    }
}
