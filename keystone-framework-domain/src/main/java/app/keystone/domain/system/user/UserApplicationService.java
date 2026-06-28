package app.keystone.domain.system.user;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.common.dto.CurrentLoginUserDTO;
import app.keystone.domain.system.post.dto.PostDTO;
import app.keystone.domain.system.role.dto.RoleDTO;
import app.keystone.domain.system.user.command.AddUserCommand;
import app.keystone.domain.system.user.command.ChangeStatusCommand;
import app.keystone.domain.system.user.command.ResetPasswordCommand;
import app.keystone.domain.system.user.command.UpdateProfileCommand;
import app.keystone.domain.system.user.command.UpdateUserAvatarCommand;
import app.keystone.domain.system.user.command.UpdateUserCommand;
import app.keystone.domain.system.user.command.UpdateUserPasswordCommand;
import app.keystone.domain.system.user.db.SearchUserDO;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.domain.system.user.dto.UserDTO;
import app.keystone.domain.system.user.dto.UserDetailDTO;
import app.keystone.domain.system.user.dto.UserProfileDTO;
import app.keystone.domain.system.user.model.UserModel;
import app.keystone.domain.system.user.model.UserModelFactory;
import app.keystone.domain.system.user.query.SearchUserQuery;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final SysUserService userService;

    private final SysRoleService roleService;

    private final SysPostService postService;

    private final UserModelFactory userModelFactory;

    public PageDTO<UserDTO> getUserList(SearchUserQuery<SearchUserDO> query) {
        Page<SearchUserDO> userPage = userService.getUserList(query);
        List<UserDTO> userDTOList = userPage.getRecords().stream().map(UserDTO::new).collect(Collectors.toList());
        return new PageDTO<>(userDTOList, userPage.getTotal());
    }

    public List<UserDTO> exportUsers(SearchUserQuery<SearchUserDO> query) {
        return userService.listUsersByQuery(query).stream().map(UserDTO::new).collect(Collectors.toList());
    }

    public UserProfileDTO getUserProfile(Long userId) {

        SysUserEntity userEntity = userService.getById(userId);
        SysPostEntity postEntity = userService.getPostOfUser(userId);
        SysRoleEntity roleEntity = userService.getRoleOfUser(userId);

        return new UserProfileDTO(userEntity, postEntity, roleEntity);
    }


    /**
     * 获取当前登录用户信息
     *
     * @return 当前登录用户信息
     */
    public CurrentLoginUserDTO getLoginUserInfo(SystemLoginUser loginUser) {
        CurrentLoginUserDTO permissionDTO = new CurrentLoginUserDTO();

        permissionDTO.setUserInfo(new UserDTO(CacheCenter.userCache().get(loginUser.getUserId())));
        permissionDTO.setRoleKey(loginUser.getRoleInfo().getRoleKey());
        permissionDTO.setPermissions(loginUser.getRoleInfo().getMenuPermissions());

        return permissionDTO;
    }


    public void updateUserProfile(UpdateProfileCommand command) {
        UserModel userModel = userModelFactory.loadById(command.getUserId());
        userModel.loadUpdateProfileCommand(command);

        userModel.checkPhoneNumberIsUnique();
        userModel.checkEmailIsUnique();

        userModel.updateById();

        CacheCenter.userCache().delete(userModel.getUserId());
    }

    public UserDetailDTO getUserDetailInfo(Long userId) {
        SysUserEntity userEntity = userService.getById(userId);
        UserDetailDTO detailDTO = new UserDetailDTO();

        LambdaQueryWrapper<SysRoleEntity> roleQuery = new LambdaQueryWrapper<SysRoleEntity>()
            .orderByAsc(SysRoleEntity::getRoleSort);
        List<RoleDTO> roleDtoList = roleService.list(roleQuery).stream().map(RoleDTO::new).collect(Collectors.toList());
        List<PostDTO> postDtoList = postService.list().stream().map(PostDTO::new).collect(Collectors.toList());
        detailDTO.setRoleOptions(roleDtoList);
        detailDTO.setPostOptions(postDtoList);

        if (userEntity != null) {
            detailDTO.setUser(new UserDTO(userEntity));
            detailDTO.setRoleId(userEntity.getRoleId());
            detailDTO.setPostId(userEntity.getPostId());
        }
        return detailDTO;
    }

    public void addUser(AddUserCommand command) {
        UserModel model = userModelFactory.create();
        model.loadAddUserCommand(command);

        model.checkUsernameIsUnique();
        model.checkPhoneNumberIsUnique();
        model.checkEmailIsUnique();
        model.checkFieldRelatedEntityExist();
        model.resetPassword(command.getPassword());

        model.insert();
    }

    public void updateUser(UpdateUserCommand command) {
        UserModel model = userModelFactory.loadById(command.getUserId());
        model.loadUpdateUserCommand(command);

        model.checkUsernameIsUnique();
        model.checkPhoneNumberIsUnique();
        model.checkEmailIsUnique();
        model.checkFieldRelatedEntityExist();
        model.updateById();

        CacheCenter.userCache().delete(model.getUserId());
    }

    public void deleteUsers(SystemLoginUser loginUser, BulkOperationCommand<Long> command) {
        for (Long id : command.getIds()) {
            UserModel userModel = userModelFactory.loadById(id);
            userModel.checkCanBeDelete(loginUser);
            userModel.deleteById();
        }
    }

    public void updatePasswordBySelf(SystemLoginUser loginUser, UpdateUserPasswordCommand command) {
        UserModel userModel = userModelFactory.loadById(command.getUserId());
        userModel.modifyPassword(command);
        userModel.updateById();

        CacheCenter.userCache().delete(userModel.getUserId());
    }

    public void resetUserPassword(ResetPasswordCommand command) {
        UserModel userModel = userModelFactory.loadById(command.getUserId());

        userModel.resetPassword(command.getPassword());
        userModel.updateById();

        CacheCenter.userCache().delete(userModel.getUserId());
    }

    public void changeUserStatus(ChangeStatusCommand command) {
        UserModel userModel = userModelFactory.loadById(command.getUserId());

        userModel.setStatus(command.getStatus());
        userModel.updateById();

        CacheCenter.userCache().delete(userModel.getUserId());
    }

    public void updateUserAvatar(UpdateUserAvatarCommand command) {
        UserModel userModel = userModelFactory.loadById(command.getUserId());

        userModel.setAvatar(command.getAvatar());
        userModel.updateById();

        CacheCenter.userCache().delete(userModel.getUserId());
    }


}
