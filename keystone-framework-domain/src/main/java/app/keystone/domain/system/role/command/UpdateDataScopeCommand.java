package app.keystone.domain.system.role.command;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateDataScopeCommand {

    @Positive
    private Long roleId;

    @NotNull(message = "部门ID列表不能为空")
    @NotEmpty(message = "部门ID列表不能为空")
    private List<@Positive Long> deptIds;

    @NotNull(message = "数据范围不能为空")
    @Min(value = 1, message = "数据范围值无效")
    @Max(value = 5, message = "数据范围值无效")
    private Integer dataScope;


}
