package app.keystone.domain.system.job.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Update scheduled job status command.
 * @author likanug
 */
@Data
@Schema(description = "修改定时任务状态")
public class UpdateJobStatusCommand {

    @NotNull
    @Min(value = 0, message = "任务状态值无效")
    @Max(value = 1, message = "任务状态值无效")
    @Schema(description = "状态（1正常 0暂停）")
    private Integer status;
}
