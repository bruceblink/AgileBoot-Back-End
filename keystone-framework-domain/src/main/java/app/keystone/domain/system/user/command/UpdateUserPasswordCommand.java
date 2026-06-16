package app.keystone.domain.system.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Size(min = 8, max = 32, message = "新密码长度必须大于等于8且小于等于32个字符")
    @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).*$",
        message = "新密码首尾不能包含空格，且必须包含字母、数字和特殊符号")
    private String newPassword;

    @NotBlank(message = "旧密码不能为空")
    @Size(max = 128, message = "旧密码长度不能超过128个字符")
    private String oldPassword;

}
