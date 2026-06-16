package app.keystone.domain.system.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateUserPasswordCommand {

    @Positive(message = "用户ID必须为正数")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 128, message = "新密码长度不能超过128个字符")
    private String newPassword;

    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128, message = "旧密码长度不能超过128个字符")
    private String oldPassword;

}
