package app.keystone.domain.system.dept.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateDeptCommand extends AddDeptCommand {

    @NotNull
    @Positive
    private Long deptId;

}
