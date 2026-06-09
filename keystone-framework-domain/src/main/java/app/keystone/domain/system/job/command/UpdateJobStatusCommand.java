package app.keystone.domain.system.job.command;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "状态（1正常 0暂停）")
    private Integer status;
}
