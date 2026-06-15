package app.keystone.domain.system.user.model;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.domain.system.dept.model.DeptModelFactory;
import app.keystone.domain.system.post.model.PostModelFactory;
import app.keystone.domain.system.role.model.RoleModelFactory;
import app.keystone.domain.system.user.command.AddUserCommand;
import app.keystone.domain.system.user.command.UpdateProfileCommand;
import app.keystone.domain.system.user.command.UpdateUserCommand;
import app.keystone.domain.system.user.command.UpdateUserPasswordCommand;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UserModel extends SysUserEntity {

    private static final long serialVersionUID = 1L;

    private transient SysUserService userService;

    private transient PostModelFactory postModelFactory;

    private transient DeptModelFactory deptModelFactory;

    private transient RoleModelFactory roleModelFactory;

    public UserModel(SysUserEntity entity, SysUserService userService, PostModelFactory postModelFactory,
        DeptModelFactory deptModelFactory, RoleModelFactory roleModelFactory) {
        this(userService, postModelFactory, deptModelFactory, roleModelFactory);

        if (entity != null) {
            BeanUtils.copyProperties(entity, this);
        }
    }

    public UserModel(SysUserService userService, PostModelFactory postModelFactory,
        DeptModelFactory deptModelFactory, RoleModelFactory roleModelFactory) {
        this.userService = userService;
        this.postModelFactory = postModelFactory;
        this.deptModelFactory = deptModelFactory;
        this.roleModelFactory = roleModelFactory;
    }

    public void loadAddUserCommand(AddUserCommand command) {
        if (command != null) {
            BeanUtils.copyProperties(command, this, "userId");
            normalizeBlankEmail();
        }
    }


    public void loadUpdateUserCommand(UpdateUserCommand command) {
        if (command != null) {
            loadAddUserCommand(command);
        }
    }

    public void loadUpdateProfileCommand(UpdateProfileCommand command) {
        if (command != null) {
            this.setSex(command.getSex());
            this.setNickname(command.getNickName());
            this.setPhoneNumber(command.getPhoneNumber());
            this.setEmail(StringUtils.trimToNull(command.getEmail()));
        }
    }


    public void checkUsernameIsUnique() {
        if (StringUtils.isNotEmpty(getUsername()) && userService.isUserNameDuplicated(getUsername(), getUserId())) {
            throw new ApiException(ErrorCode.Business.USER_NAME_IS_NOT_UNIQUE);
        }
    }

    public void checkPhoneNumberIsUnique() {
        if (StringUtils.isNotEmpty(getPhoneNumber()) && userService.isPhoneDuplicated(getPhoneNumber(),
            getUserId())) {
            throw new ApiException(ErrorCode.Business.USER_PHONE_NUMBER_IS_NOT_UNIQUE);
        }
    }

    public void checkFieldRelatedEntityExist() {

        if (getPostId() != null) {
            postModelFactory.loadById(getPostId());
        }

        if (getDeptId() != null) {
            deptModelFactory.loadById(getDeptId());
        }

        if (getRoleId() != null) {
            roleModelFactory.loadById(getRoleId());
        }

    }


    public void checkEmailIsUnique() {
        if (StringUtils.isNotEmpty(getEmail()) && userService.isEmailDuplicated(getEmail(), getUserId())) {
            throw new ApiException(ErrorCode.Business.USER_EMAIL_IS_NOT_UNIQUE);
        }
    }

    private void normalizeBlankEmail() {
        setEmail(StringUtils.trimToNull(getEmail()));
    }

    public void checkCanBeDelete(SystemLoginUser loginUser) {
        if (Objects.equals(getUserId(), loginUser.getUserId())
            || Boolean.TRUE.equals(this.getIsAdmin())) {
            throw new ApiException(ErrorCode.Business.USER_CURRENT_USER_CAN_NOT_BE_DELETE);
        }
    }


    public void modifyPassword(UpdateUserPasswordCommand command) {
        if (!AuthenticationUtils.matchesPassword(command.getOldPassword(), getPassword())) {
            throw new ApiException(ErrorCode.Business.USER_PASSWORD_IS_NOT_CORRECT);
        }

        if (AuthenticationUtils.matchesPassword(command.getNewPassword(), getPassword())) {
            throw new ApiException(ErrorCode.Business.USER_NEW_PASSWORD_IS_THE_SAME_AS_OLD);
        }
        setPassword(AuthenticationUtils.encryptPassword(command.getNewPassword()));
    }

    public void resetPassword(String newPassword) {
        setPassword(AuthenticationUtils.encryptPassword(newPassword));
    }

    @Override
    public boolean updateById() {
        if (Boolean.TRUE.equals(this.getIsAdmin()) && KeystoneConfig.isDemoEnabled()) {
            throw new ApiException(Business.USER_ADMIN_CAN_NOT_BE_MODIFY);
        }

       return super.updateById();
    }

}
