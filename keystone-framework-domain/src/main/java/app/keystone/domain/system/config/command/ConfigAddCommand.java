package app.keystone.domain.system.config.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * 新增参数配置命令
 * @author likanug
 */
@Data
@Schema(description = "新增参数配置")
public class ConfigAddCommand {

    @NotBlank
    @Schema(description = "配置名称")
    private String configName;

    @NotBlank
    @Schema(description = "配置键名")
    private String configKey;

    @Schema(description = "可选值列表")
    private List<String> configOptions;

    @NotBlank
    @Schema(description = "配置值")
    private String configValue;

    @NotNull
    @Min(value = 0, message = "是否允许修改值无效")
    @Max(value = 1, message = "是否允许修改值无效")
    @Schema(description = "是否允许修改（1是 0否）")
    private Integer isAllowChange;

    @Schema(description = "备注")
    private String remark;
}
