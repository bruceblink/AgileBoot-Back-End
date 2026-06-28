package app.keystone.integrationTest.db;

import app.keystone.domain.system.role.query.AllocatedRoleQuery;
import app.keystone.domain.system.role.query.UnallocatedRoleQuery;
import app.keystone.domain.system.menu.db.SysMenuEntity;
import app.keystone.domain.system.menu.db.SysMenuService;
import app.keystone.domain.system.post.db.SysPostEntity;
import app.keystone.domain.system.role.db.SysRoleEntity;
import app.keystone.domain.system.user.db.SearchUserDO;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.domain.system.user.query.SearchUserQuery;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;

@DockerMySqlIntegrationTest
class SysUserServiceImplTest {

    @Resource
    SysUserService userService;
    @Resource
    SysMenuService menuService;

    @Test
    @Rollback
    void testIsUserNameDuplicated() {
        boolean addWithSame = userService.isUserNameDuplicated("admin", null);
        boolean updateWithSame = userService.isUserNameDuplicated("admin", 1L);
        boolean addWithoutSame = userService.isUserNameDuplicated("admin1", null);

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }


    @Test
    @Rollback
    void testIsPhoneDuplicated() {
        boolean addWithSame = userService.isPhoneDuplicated("15888888883", null);
        boolean updateWithSame = userService.isPhoneDuplicated("15888888883", 1L);
        boolean addWithoutSame = userService.isPhoneDuplicated("15888888899", null);

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }


    @Test
    @Rollback
    void testIsEmailDuplicated() {
        boolean addWithSame = userService.isEmailDuplicated("keystone@163.com", null);
        boolean updateWithSame = userService.isEmailDuplicated("keystone@163.com", 1L);
        boolean addWithoutSame = userService.isEmailDuplicated("keystone@164.com", null);

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }

    @Test
    @Rollback
    void testGetRoleOfUser() {
        SysRoleEntity roleOfUser = userService.getRoleOfUser(1L);

        Assertions.assertEquals(1L, roleOfUser.getRoleId());
        Assertions.assertEquals("超级管理员", roleOfUser.getRoleName());
        Assertions.assertEquals("admin", roleOfUser.getRoleKey());
    }

    @Test
    @Rollback
    void testGetPostOfUser() {
        SysPostEntity postOfUser = userService.getPostOfUser(1L);

        Assertions.assertEquals(1L, postOfUser.getPostId());
        Assertions.assertEquals("董事长", postOfUser.getPostName());
        Assertions.assertEquals("ceo", postOfUser.getPostCode());
    }

    @Test
    @Rollback
    void testGetMenuPermissionsForUsers() {
        Set<String> permissionByUser = userService.getMenuPermissionsForUser(2L);
        List<SysMenuEntity> allMenus = menuService.list();
        Set<String> userPermissions = permissionByUser.stream()
            .filter(permission -> permission != null && !permission.isBlank())
            .collect(Collectors.toSet());
        Set<String> allPermissions = allMenus.stream()
            .map(SysMenuEntity::getPermission)
            .filter(permission -> permission != null && !permission.isBlank())
            .collect(Collectors.toSet());

        Assertions.assertFalse(userPermissions.isEmpty());
        Assertions.assertTrue(allPermissions.containsAll(userPermissions));
        Assertions.assertTrue(userPermissions.contains("system:user:list"));
        Assertions.assertTrue(allPermissions.size() > userPermissions.size());
    }


    @Test
    @Rollback
    void testGetUserByUserName() {
        SysUserEntity admin = userService.getUserByUserName("admin");

        Assertions.assertEquals(1L, admin.getUserId());
        Assertions.assertEquals(1L, admin.getRoleId());
        Assertions.assertEquals(1L, admin.getPostId());
    }

    @Test
    @Rollback
    void testGetUserByExternalSubject() {
        SysUserEntity admin = userService.getUserByUserName("admin");
        String externalSubject = "external-subject-test-001";

        admin.setExternalSubject(externalSubject);
        userService.updateById(admin);

        SysUserEntity mappedUser = userService.getUserByExternalSubject(externalSubject);
        SysUserEntity nonExistUser = userService.getUserByExternalSubject("external-subject-not-exist");

        Assertions.assertNotNull(mappedUser);
        Assertions.assertEquals(admin.getUserId(), mappedUser.getUserId());
        Assertions.assertEquals(externalSubject, mappedUser.getExternalSubject());
        Assertions.assertNull(nonExistUser);
    }

    @Test
    @Rollback
    void testGetRoleAssignedUserList() {
        AllocatedRoleQuery allocatedRoleQuery = new AllocatedRoleQuery();
        allocatedRoleQuery.setRoleId(1L);
        allocatedRoleQuery.setUsername("admin");

        Page<SysUserEntity> roleAssignedPage = userService.getUserListByRole(allocatedRoleQuery);

        Assertions.assertEquals(1, roleAssignedPage.getTotal());
        Assertions.assertEquals("admin", roleAssignedPage.getRecords().get(0).getUsername());
    }


    @Test
    @Rollback
    void testGetRoleUnassignedUserList() {
        UnallocatedRoleQuery unallocatedRoleQuery = new UnallocatedRoleQuery();
        unallocatedRoleQuery.setRoleId(3L);
        unallocatedRoleQuery.setUsername("ag2");

        Page<SysUserEntity> roleAssignedPage = userService.getUserListByRole(unallocatedRoleQuery);

        Assertions.assertEquals(1, roleAssignedPage.getTotal());
        Assertions.assertEquals("ag2", roleAssignedPage.getRecords().get(0).getUsername());
    }

    @Test
    @Rollback
    void testGetUserList() {
        SearchUserQuery<SearchUserDO> searchUserQuery = new SearchUserQuery<>();
        searchUserQuery.setUsername("a");

        Page<SearchUserDO> userList = userService.getUserList(searchUserQuery);

        Assertions.assertEquals(3, userList.getTotal());
        Assertions.assertEquals("admin", userList.getRecords().get(0).getUsername());
    }


}
