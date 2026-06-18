package app.keystone.integrationTest.db;

import app.keystone.domain.system.menu.db.SysMenuEntity;
import app.keystone.domain.system.menu.db.SysMenuService;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;

@DockerMySqlIntegrationTest
class SysMenuServiceImplTest {

    @Resource
    SysMenuService menuService;

    @Test
    @Rollback
    void testGetMenuListByUserId() {
        List<SysMenuEntity> userMenus = menuService.getMenuListByUserId(2L);
        List<SysMenuEntity> allMenus = menuService.list();

        Assertions.assertFalse(userMenus.isEmpty());
        Assertions.assertTrue(allMenus.size() > userMenus.size());
        Assertions.assertTrue(userMenus.stream().anyMatch(menu -> "用户管理".equals(menu.getMenuName())));
    }

    @Test
    @Rollback
    void testGetMenuIdsByRoleId() {
        List<Long> roleMenuIds = menuService.getMenuIdsByRoleId(2L);
        List<SysMenuEntity> allMenus = menuService.list();
        Set<Long> allMenuIds = allMenus.stream().map(SysMenuEntity::getMenuId).collect(Collectors.toSet());

        Assertions.assertFalse(roleMenuIds.isEmpty());
        Assertions.assertTrue(allMenuIds.containsAll(roleMenuIds));
        Assertions.assertTrue(allMenus.size() > roleMenuIds.size());
    }

    @Test
    @Rollback
    void testIsMenuNameDuplicated() {
        boolean addWithSame = menuService.isMenuNameDuplicated("用户管理", null, 1L);
        boolean updateWithSame = menuService.isMenuNameDuplicated("用户管理", 5L, 1L);
        boolean addWithoutSame = menuService.isMenuNameDuplicated("用户管理", null, 2L);

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }

    @Test
    @Rollback
    void testHasChildrenMenus() {
        boolean hasChildrenMenu = menuService.hasChildrenMenu(5L);
        boolean hasNotChildrenMenu = menuService.hasChildrenMenu(20L);

        Assertions.assertTrue(hasChildrenMenu);
        Assertions.assertFalse(hasNotChildrenMenu);
    }

    @Test
    @Rollback
    void testIsMenuAssignToRole() {
        List<SysMenuEntity> allMenus = menuService.list();

        boolean isAssignToRole = menuService.isMenuAssignToRoles(allMenus.get(0).getMenuId());
        // role2 默认不给最后一个权限 所以最后一个菜单无权限
        boolean isNotAssignToRole = menuService.isMenuAssignToRoles(allMenus.get(allMenus.size() - 1).getMenuId());

        Assertions.assertFalse(isNotAssignToRole);
        Assertions.assertTrue(isAssignToRole);
    }



}
