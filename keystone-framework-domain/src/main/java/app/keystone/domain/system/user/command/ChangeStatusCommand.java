package app.keystone.domain.system.user.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class ChangeStatusCommand {

    @Positive(message = "用户ID必须为正数")
    private Long userId;

    @NotNull(message = "用户状态不能为空")
    @Min(value = 0, message = "用户状态值无效")
    @Max(value = 1, message = "用户状态值无效")
    private Integer status;

}
