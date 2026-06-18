package app.keystone.integrationTest.db;

import app.keystone.domain.system.post.db.SysPostService;
import app.keystone.integrationTest.DockerMySqlIntegrationTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.Rollback;

@DockerMySqlIntegrationTest
class SysPostServiceImplTest {

    @Resource
    SysPostService postService;

    @Test
    @Rollback
    void testIsPostNameDuplicated() {
        boolean addWithSame = postService.isPostNameDuplicated(null, "董事长");
        boolean updateWithSame = postService.isPostNameDuplicated(1L, "董事长");
        boolean addWithoutSame = postService.isPostNameDuplicated(null, "董事长1");

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }


    @Test
    @Rollback
    void testIsPostCodeDuplicated() {
        boolean addWithSame = postService.isPostCodeDuplicated(null, "ceo");
        boolean updateWithSame = postService.isPostCodeDuplicated(1L, "ceo");
        boolean addWithoutSame = postService.isPostCodeDuplicated(null, "ceo1");

        Assertions.assertTrue(addWithSame);
        Assertions.assertFalse(updateWithSame);
        Assertions.assertFalse(addWithoutSame);
    }


    @Test
    @Rollback
    void testIsAssignedToUsers() {
        boolean assignedPost = postService.isAssignedToUsers(1L);
        boolean unassignedPost = postService.isAssignedToUsers(3L);

        Assertions.assertTrue(assignedPost);
        Assertions.assertFalse(unassignedPost);
    }


}
