package app.keystone.domain.system.role.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateRoleCommand extends AddRoleCommand {

    @NotNull(message = "角色ID不能为空")
    @Positive(message = "角色ID必须为正数")
    private Long roleId;

}
