package app.keystone.domain.system.role;

import app.keystone.common.core.page.PageDTO;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.system.role.command.AddRoleCommand;
import app.keystone.domain.system.role.command.UpdateDataScopeCommand;
import app.keystone.domain.system.role.command.UpdateRoleCommand;
import app.keystone.domain.system.role.command.UpdateStatusCommand;
import app.keystone.domain.system.role.dto.RoleDTO;
import app.keystone.domain.system.role.model.RoleModel;
import app.keystone.domain.system.role.model.RoleModelFactory;
import app.keystone.domain.system.role.query.AllocatedRoleQuery;
import app.keystone.domain.system.role.query.RoleQuery;
import app.keystone.domain.system.role.query.UnallocatedRoleQuery;
import app.keystone.domain.system.user.dto.UserDTO;
import app.keystone.domain.system.user.model.UserModel;
import app.keystone.domain.system.user.model.UserModelFactory;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.menu.db.SysMenuService;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.db.SysUserService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author likanug
 */
@Service
@RequiredArgsConstructor
public class RoleApplicationService {

    private final RoleModelFactory roleModelFactory;

    private final UserModelFactory userModelFactory;

    private final SysRoleService roleService;

    private final SysUserService userService;

    private final SysMenuService menuService;


    public PageDTO<RoleDTO> getRoleList(RoleQuery query) {
        Page<SysRoleEntity> page = roleService.page(query.toPage(), query.toQueryWrapper());
        List<RoleDTO> records = page.getRecords().stream().map(RoleDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public List<RoleDTO> exportRoles(RoleQuery query) {
        return roleService.list(query.toQueryWrapper()).stream().map(RoleDTO::new).collect(Collectors.toList());
    }

    public RoleDTO getRoleInfo(Long roleId) {
        SysRoleEntity byId = roleService.getById(roleId);
        RoleDTO roleDTO = new RoleDTO(byId);
        List<Long> selectedDeptList = parseDeptIds(byId.getDeptIdSet());
        roleDTO.setSelectedDeptList(selectedDeptList);
        roleDTO.setSelectedMenuList(menuService.getMenuIdsByRoleId(roleId));
        return roleDTO;
    }


    public void addRole(AddRoleCommand addCommand) {
        RoleModel roleModel = roleModelFactory.create();
        roleModel.loadAddCommand(addCommand);

        roleModel.checkRoleNameUnique();
        roleModel.checkRoleKeyUnique();

        roleModel.insert();
    }

    public void deleteRoleByBulk(List<Long> roleIds) {
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                RoleModel roleModel = roleModelFactory.loadById(roleId);

                roleModel.checkRoleCanBeDelete();

                roleModel.deleteById();
            }
        }
    }


    public void updateRole(UpdateRoleCommand updateCommand) {
        RoleModel roleModel = roleModelFactory.loadById(updateCommand.getRoleId());
        roleModel.loadUpdateCommand(updateCommand);

        roleModel.checkRoleKeyUnique();
        roleModel.checkRoleNameUnique();

        roleModel.updateById();
    }

    public void updateStatus(UpdateStatusCommand command) {
        RoleModel roleModel = roleModelFactory.loadById(command.getRoleId());

        roleModel.setStatus(command.getStatus());

        roleModel.updateById();
    }

    public void updateDataScope(UpdateDataScopeCommand command) {
        RoleModel roleModel = roleModelFactory.loadById(command.getRoleId());

        roleModel.setDeptIds(command.getDeptIds());
        roleModel.setDataScope(command.getDataScope());
        roleModel.generateDeptIdSet();

        roleModel.updateById();
    }


    public PageDTO<UserDTO> getAllocatedUserList(AllocatedRoleQuery query) {
        Page<SysUserEntity> page = userService.getUserListByRole(query);
        List<UserDTO> dtoList = page.getRecords().stream().map(UserDTO::new).collect(Collectors.toList());
        return new PageDTO<>(dtoList, page.getTotal());
    }

    public PageDTO<UserDTO> getUnallocatedUserList(UnallocatedRoleQuery query) {
        Page<SysUserEntity> page = userService.getUserListByRole(query);
        List<UserDTO> dtoList = page.getRecords().stream().map(UserDTO::new).collect(Collectors.toList());
        return new PageDTO<>(dtoList, page.getTotal());
    }

    public void deleteRoleOfUserByBulk(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            LambdaUpdateWrapper<SysUserEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(SysUserEntity::getRoleId, null).eq(SysUserEntity::getUserId, userId);

            userService.update(updateWrapper);

            CacheCenter.userCache().delete(userId);
        }
    }

    public void addRoleOfUserByBulk(Long roleId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        RoleModel roleModel = roleModelFactory.loadById(roleId);
        roleModel.checkRoleAvailable();

        for (Long userId : userIds) {
            UserModel user = userModelFactory.loadById(userId);
            user.setRoleId(roleId);
            user.updateById();

            CacheCenter.userCache().delete(userId);
        }
    }

    private List<Long> parseDeptIds(String deptIdSet) {
        if (StringUtils.isBlank(deptIdSet)) {
            return Collections.emptyList();
        }

        return Arrays.stream(deptIdSet.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }


}
