package app.keystone.domain.system.job.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Update scheduled job command.
 * @author likanug
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "修改定时任务")
public class UpdateJobCommand extends AddJobCommand {

    @NotNull
    @Positive
    @Schema(description = "任务ID")
    private Long jobId;
}
