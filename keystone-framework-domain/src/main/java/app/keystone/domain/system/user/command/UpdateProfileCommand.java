package app.keystone.domain.system.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateProfileCommand {

    @Positive(message = "用户ID必须为正数")
    private Long userId;

    @Min(value = 0, message = "用户性别值无效")
    @Max(value = 2, message = "用户性别值无效")
    private Integer sex;

    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 32, message = "用户昵称长度不能超过32个字符")
    private String nickName;

    @Size(max = 18, message = "电话号码长度不能超过18个字符")
    private String phoneNumber;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

}
