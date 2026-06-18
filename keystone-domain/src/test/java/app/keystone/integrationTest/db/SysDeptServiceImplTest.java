package app.keystone.integrationTest.db;

import app.keystone.domain.system.dept.db.SysDeptService;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;

@DockerMySqlIntegrationTest
class SysDeptServiceImplTest {

    @Resource
    SysDeptService deptService;

    @Test
    @Rollback
    void testIsDeptNameDuplicated() {
        boolean addWithSame = deptService.isDeptNameDuplicated("Keystone科技", null, null);
        boolean updateWithSame = deptService.isDeptNameDuplicated("Keystone科技", 1L, null);
        boolean addSameInParent = deptService.isDeptNameDuplicated("深圳总公司", null, 1L);

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertTrue(addSameInParent);
    }


    @Test
    @Rollback
    void testHasChildDept() {
        boolean hasChild = deptService.hasChildrenDept(3L, null);
        boolean hasDisableChild = deptService.hasChildrenDept(3L, false);

        Assertions.assertTrue(hasChild);
        Assertions.assertFalse(hasDisableChild);
    }


    @Test
    @Rollback
    void testIsChildOfTheDept() {
        boolean isIndirectChild = deptService.isChildOfTheDept(1L, 10L);
        boolean isDirectChild = deptService.isChildOfTheDept(3L, 10L);
        boolean isNotChild = deptService.isChildOfTheDept(5L, 10L);

        Assertions.assertTrue(isIndirectChild);
        Assertions.assertTrue(isDirectChild);
        Assertions.assertFalse(isNotChild);
    }


    @Test
    @Rollback
    void testIsDeptAssignedToUsers() {
        boolean notAssigned = deptService.isDeptAssignedToUsers(1L);
        boolean isAssigned = deptService.isDeptAssignedToUsers(4L);

        Assertions.assertFalse(notAssigned);
        Assertions.assertTrue(isAssigned);
    }


}
