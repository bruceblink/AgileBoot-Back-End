package app.keystone.domain.system.role.model;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.role.db.SysRoleMenuEntity;
import app.keystone.domain.system.role.db.SysRoleMenuService;
import app.keystone.domain.system.role.db.SysRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 角色模型工厂
 * @author likanug
 */
@Component
@RequiredArgsConstructor
public class RoleModelFactory {

    private final SysRoleService roleService;

    private final SysRoleMenuService roleMenuService;

    public RoleModel loadById(Long roleId) {
        SysRoleEntity byId = roleService.getById(roleId);
        if (byId == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, roleId, "角色");
        }

        LambdaQueryWrapper<SysRoleMenuEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRoleMenuEntity::getRoleId, roleId);
        List<Long> menuIds = roleMenuService.list(queryWrapper).stream().map(SysRoleMenuEntity::getMenuId)
            .collect(Collectors.toList());
        List<Long> deptIds = parseDeptIds(byId.getDeptIdSet());

        RoleModel roleModel = new RoleModel(byId, roleService, roleMenuService);

        roleModel.setMenuIds(menuIds);
        roleModel.setDeptIds(deptIds);

        return roleModel;
    }

    public RoleModel create() {
        return new RoleModel(roleService, roleMenuService);
    }

    private List<Long> parseDeptIds(String deptIdSet) {
        if (StringUtils.isBlank(deptIdSet)) {
            return Collections.emptyList();
        }

        return Arrays.stream(deptIdSet.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .map(Long::valueOf)
            .collect(Collectors.toList());
    }


}
