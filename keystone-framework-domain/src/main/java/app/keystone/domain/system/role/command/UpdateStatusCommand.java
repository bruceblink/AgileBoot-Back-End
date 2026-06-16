package app.keystone.domain.system.role.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@NoArgsConstructor
public class UpdateStatusCommand {

    @Positive
    private Long roleId;

    @NotNull(message = "角色状态不能为空")
    @Min(value = 0, message = "角色状态值无效")
    @Max(value = 1, message = "角色状态值无效")
    private Integer status;

}
