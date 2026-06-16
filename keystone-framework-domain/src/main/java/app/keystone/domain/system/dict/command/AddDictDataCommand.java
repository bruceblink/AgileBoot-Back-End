package app.keystone.domain.system.dict.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增字典数据命令
 * @author likanug
 */
@Data
@Schema(description = "新增字典数据")
public class AddDictDataCommand {

    @NotBlank
    @Schema(description = "字典类型")
    private String dictType;

    @NotBlank
    @Schema(description = "字典标签")
    private String dictLabel;

    @NotBlank
    @Schema(description = "字典键值")
    private String dictValue;

    @Schema(description = "字典排序")
    private Integer dictSort;

    @Schema(description = "是否默认（1是 0否）")
    private Integer isDefault;

    @Schema(description = "样式属性")
    private String cssClass;

    @Schema(description = "表格回显样式")
    private String listClass;

    @NotNull
    @Min(value = 0, message = "字典状态值无效")
    @Max(value = 1, message = "字典状态值无效")
    @Schema(description = "状态（1正常 0停用）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
