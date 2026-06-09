package app.keystone.domain.system.dict.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 修改字典类型命令
 * @author likanug
 */
@Data
@Schema(description = "修改字典类型")
public class UpdateDictTypeCommand {

    @NotNull
    @Positive
    @Schema(description = "字典主键")
    private Long dictId;

    @NotBlank
    @Schema(description = "字典名称")
    private String dictName;

    @NotBlank
    @Schema(description = "字典类型")
    private String dictType;

    @NotNull
    @Schema(description = "状态（1正常 0停用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
