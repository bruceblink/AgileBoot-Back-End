package app.keystone.domain.system.role.command;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateDataScopeCommand {

    @NotNull
    @Positive
    private Long roleId;

    @NotNull
    @NotEmpty
    private List<Long> deptIds;

    private Integer dataScope;


}
