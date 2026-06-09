package app.keystone.admin.customize.service.login;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.DataScopeEnum;
import app.keystone.common.enums.common.UserStatusEnum;
import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.domain.system.menu.db.SysMenuEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.menu.db.SysMenuService;
import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.domain.system.user.db.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义加载用户信息通过用户名
 * 用于SpringSecurity 登录流程
 * 没有办法把这个类 放进loginService中  会在SecurityConfig中造成循环依赖
 * @author likanug
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService userService;

    private final SysMenuService menuService;

    private final SysRoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserEntity userEntity = userService.getUserByUserName(username);
        if (userEntity == null) {
            log.info("登录用户：{} 不存在.", username);
            throw new ApiException(ErrorCode.Business.USER_NON_EXIST, username);
        }
        if (!Objects.equals(UserStatusEnum.NORMAL.getValue(), userEntity.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new ApiException(ErrorCode.Business.USER_IS_DISABLE, username);
        }

        return buildLoginUser(userEntity);
    }

    public SystemLoginUser buildLoginUser(SysUserEntity userEntity) {
        RoleInfo roleInfo = getRoleInfo(userEntity.getRoleId(), userEntity.getIsAdmin());
        SystemLoginUser loginUser = new SystemLoginUser(userEntity.getUserId(), userEntity.getIsAdmin(), userEntity.getUsername(),
            userEntity.getPassword(), roleInfo, userEntity.getDeptId());
        loginUser.fillLoginInfo();
        return loginUser;
    }

    public RoleInfo getRoleInfo(Long roleId, boolean isAdmin) {
        if (roleId == null) {
            return RoleInfo.EMPTY_ROLE;
        }

        if (isAdmin) {
            LambdaQueryWrapper<SysMenuEntity> menuQuery = Wrappers.lambdaQuery();
            menuQuery.select(SysMenuEntity::getMenuId);
            List<SysMenuEntity> allMenus = menuService.list(menuQuery);

            Set<Long> allMenuIds = allMenus.stream().map(SysMenuEntity::getMenuId).collect(Collectors.toSet());

            return new RoleInfo(RoleInfo.ADMIN_ROLE_ID, RoleInfo.ADMIN_ROLE_KEY, DataScopeEnum.ALL, SetUtils.emptySet(),
                RoleInfo.ADMIN_PERMISSIONS, allMenuIds);

        }

        SysRoleEntity roleEntity = roleService.getById(roleId);

        if (roleEntity == null) {
            return RoleInfo.EMPTY_ROLE;
        }

        List<SysMenuEntity> menuList = roleService.getMenuListByRoleId(roleId);

        Set<Long> menuIds = menuList.stream().map(SysMenuEntity::getMenuId).collect(Collectors.toSet());
        Set<String> permissions = menuList.stream().map(SysMenuEntity::getPermission).collect(Collectors.toSet());

        DataScopeEnum dataScopeEnum = BasicEnumUtil.fromValue(DataScopeEnum.class, roleEntity.getDataScope());

        Set<Long> deptIdSet = SetUtils.emptySet();
        if (roleEntity.getDeptIdSet() != null && !roleEntity.getDeptIdSet().isEmpty()) {
            deptIdSet = java.util.Arrays.stream(roleEntity.getDeptIdSet().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        }

        return new RoleInfo(roleId, roleEntity.getRoleKey(), dataScopeEnum, deptIdSet, permissions, menuIds);
    }


}
