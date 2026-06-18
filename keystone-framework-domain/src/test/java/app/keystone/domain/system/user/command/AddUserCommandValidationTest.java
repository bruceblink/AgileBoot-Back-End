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
        command.setNickname("n".repeat(33));
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

    @Test
    void shouldRejectInvalidPassword() {
        AddUserCommand command = validAddUserCommand();

        command.setPassword("Aa1!Aa1");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Aa1!" + "a".repeat(29));
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Password!");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("12345678!");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Password123");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword(" Password123!");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Password123! ");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Password 123");
        assertTrue(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));
    }

    @Test
    void shouldAcceptValidPasswordBoundaries() {
        AddUserCommand command = validAddUserCommand();

        command.setPassword("Aa1!Aa1!");
        assertFalse(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));

        command.setPassword("Aa1!" + "a".repeat(28));
        assertFalse(violationProperties(validator.validate(command, Default.class,
            UserCommandGroups.Create.class)).contains("password"));
    }

    @Test
    void resetPasswordShouldUsePasswordRule() {
        ResetPasswordCommand command = new ResetPasswordCommand();
        command.setUserId(1L);
        command.setPassword("Password123!");
        assertTrue(validator.validate(command).isEmpty());

        command.setPassword("Password123");
        assertTrue(violationProperties(validator.validate(command)).contains("password"));
    }

    @Test
    void updatePasswordShouldUsePasswordRuleForNewPassword() {
        UpdateUserPasswordCommand command = new UpdateUserPasswordCommand();
        command.setUserId(1L);
        command.setOldPassword("old-password");
        command.setNewPassword("Password123!");
        assertTrue(validator.validate(command).isEmpty());

        command.setNewPassword("Password123");
        assertTrue(violationProperties(validator.validate(command)).contains("newPassword"));
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
