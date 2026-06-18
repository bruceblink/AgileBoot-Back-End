package app.keystone.integrationTest.db;

import app.keystone.domain.system.role.db.SysRoleService;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;

@DockerMySqlIntegrationTest
class SysRoleServiceImplTest {

    @Resource
    SysRoleService roleService;


    @Test
    @Rollback
    void testIsRoleNameDuplicated() {
        boolean addWithSame = roleService.isRoleNameDuplicated(null, "超级管理员");
        boolean updateWithSame = roleService.isRoleNameDuplicated(1L, "超级管理员");
        boolean addWithoutSame = roleService.isRoleNameDuplicated(null, "超级管理员1");

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }


    @Test
    @Rollback
    void testIsRoleKeyDuplicated() {
        boolean addWithSame = roleService.isRoleKeyDuplicated(null, "admin");
        boolean updateWithSame = roleService.isRoleKeyDuplicated(1L, "admin");
        boolean addWithoutSame = roleService.isRoleKeyDuplicated(null, "admin1");

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }



    @Test
    @Rollback
    void testIsAssignedToUsers() {
        boolean assignedRole = roleService.isAssignedToUsers(1L);
        boolean unassignedRole = roleService.isAssignedToUsers(3L);

        Assertions.assertTrue(assignedRole);
        Assertions.assertFalse(unassignedRole);
    }

}
