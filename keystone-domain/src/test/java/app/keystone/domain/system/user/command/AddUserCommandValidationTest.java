package app.keystone.domain.system.user.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AddUserCommandValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createShouldRequirePassword() {
        AddUserCommand command = validAddUserCommand();
        command.setPassword(null);

        Set<ConstraintViolation<AddUserCommand>> violations = validator.validate(command, Default.class,
            UserCommandGroups.Create.class);

        assertTrue(violationProperties(violations).contains("password"));
    }

    @Test
    void updateShouldNotRequirePassword() {
        UpdateUserCommand command = new UpdateUserCommand();
        command.setUserId(1L);
        command.setUsername("user1");
        command.setNickname("用户1");
        command.setStatus(1);

        Set<ConstraintViolation<UpdateUserCommand>> violations = validator.validate(command);

        assertFalse(violationProperties(violations).contains("password"));
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectInvalidFields() {
        AddUserCommand command = validAddUserCommand();
        command.setUsername("");
        command.setNickname("");
        command.setEmail("invalid-email");
        command.setPhoneNumber("1".repeat(19));
        command.setSex(3);
        command.setAvatar("a".repeat(513));
        command.setStatus(2);
        command.setRoleId(0L);
        command.setRemark("r".repeat(513));

        Set<String> properties = violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class));

        assertTrue(properties.contains("username"));
        assertTrue(properties.contains("nickname"));
        assertTrue(properties.contains("email"));
        assertTrue(properties.contains("phoneNumber"));
        assertTrue(properties.contains("sex"));
        assertTrue(properties.contains("avatar"));
        assertTrue(properties.contains("status"));
        assertTrue(properties.contains("roleId"));
        assertTrue(properties.contains("remark"));
    }

    private AddUserCommand validAddUserCommand() {
        AddUserCommand command = new AddUserCommand();
        command.setDeptId(1L);
        command.setUsername("user1");
        command.setNickname("用户1");
        command.setEmail("user1@example.com");
        command.setPhoneNumber("13800138000");
        command.setSex(1);
        command.setAvatar("/profile/avatar/user1.png");
        command.setPassword("Password123!");
        command.setStatus(1);
        command.setRoleId(1L);
        command.setPostId(1L);
        command.setRemark("remark");
        return command;
    }

    private <T> Set<String> violationProperties(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
