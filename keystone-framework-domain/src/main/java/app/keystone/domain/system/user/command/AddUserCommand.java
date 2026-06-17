package app.keystone.domain.system.user.command;

import app.keystone.common.annotation.ExcelColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class AddUserCommand {

    @ExcelColumn(name = "部门ID")
    private Long deptId;

    @ExcelColumn(name = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "用户名只允许字母、数字和下划线，且首字符不能为数字")
    private String username;

    @ExcelColumn(name = "昵称")
    @Size(max = 32, message = "用户昵称长度不能超过32个字符")
    private String nickname;

    @ExcelColumn(name = "邮件")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @ExcelColumn(name = "电话号码")
    @Size(max = 18, message = "电话号码长度不能超过18个字符")
    private String phoneNumber;

    @ExcelColumn(name = "性别")
    @Min(value = 0, message = "用户性别值无效")
    @Max(value = 2, message = "用户性别值无效")
    private Integer sex;

    @ExcelColumn(name = "头像")
    @Size(max = 512, message = "头像地址长度不能超过512个字符")
    private String avatar;

    @ExcelColumn(name = "密码")
    @NotBlank(message = "用户密码不能为空", groups = UserCommandGroups.Create.class)
    @Size(min = 8, max = 32, message = "用户密码长度必须大于等于8且小于等于32个字符")
    @Pattern(regexp = "^(?!\\s)(?!.*\\s$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).*$",
        message = "用户密码首尾不能包含空格，且必须包含字母、数字和特殊符号")
    private String password;

    @ExcelColumn(name = "状态")
    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "用户状态值无效")
    @Max(value = 1, message = "用户状态值无效")
    private Integer status;

    @ExcelColumn(name = "角色ID")
    @Positive(message = "角色ID必须为正数")
    private Long roleId;

    @ExcelColumn(name = "职位ID")
    private Long postId;

    @ExcelColumn(name = "备注")
    @Size(max = 512, message = "备注长度不能超过512个字符")
    private String remark;


}
