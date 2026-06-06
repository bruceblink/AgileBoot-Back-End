package app.keystone.domain.system.job.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Add scheduled job command.
 * @author likanug
 */
@Data
@Schema(description = "新增定时任务")
public class AddJobCommand {

    @NotBlank
    @Schema(description = "任务名称")
    private String jobName;

    @NotBlank
    @Schema(description = "任务组名")
    private String jobGroup;

    @NotBlank
    @Schema(description = "调用目标，格式 springBean.method()")
    private String invokeTarget;

    @NotBlank
    @Schema(description = "Cron执行表达式")
    private String cronExpression;

    @NotNull
    @Schema(description = "是否允许并发执行（1允许 0禁止）")
    private Integer concurrent;

    @NotNull
    @Schema(description = "状态（1正常 0暂停）")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
