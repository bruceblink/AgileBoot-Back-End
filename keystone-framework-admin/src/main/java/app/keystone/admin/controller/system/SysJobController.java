package app.keystone.admin.controller.system;

import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.system.job.JobApplicationService;
import app.keystone.domain.system.job.command.AddJobCommand;
import app.keystone.domain.system.job.command.UpdateJobCommand;
import app.keystone.domain.system.job.command.UpdateJobStatusCommand;
import app.keystone.domain.system.job.dto.JobDTO;
import app.keystone.domain.system.job.query.JobQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scheduled job management.
 * @author likanug
 */
@Tag(name = "定时任务API", description = "定时任务相关的增删查改和运行控制")
@RestController
@RequestMapping("/system/jobs")
@Validated
@RequiredArgsConstructor
public class SysJobController extends BaseController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "定时任务列表")
    @PreAuthorize("@permission.has('system:job:list')")
    @GetMapping
    public ResponseDTO<PageDTO<JobDTO>> list(@Validated JobQuery query) {
        return ResponseDTO.ok(jobApplicationService.getJobList(query));
    }

    @Operation(summary = "定时任务详情")
    @PreAuthorize("@permission.has('system:job:query')")
    @GetMapping("/{jobId}")
    public ResponseDTO<JobDTO> getInfo(@NotNull @Positive @PathVariable Long jobId) {
        return ResponseDTO.ok(jobApplicationService.getJobInfo(jobId));
    }

    @Operation(summary = "添加定时任务")
    @PreAuthorize("@permission.has('system:job:add')")
    @AccessLog(title = "定时任务", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<Void> add(@Valid @RequestBody AddJobCommand command) {
        jobApplicationService.addJob(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改定时任务")
    @PreAuthorize("@permission.has('system:job:edit')")
    @AccessLog(title = "定时任务", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{jobId}")
    public ResponseDTO<Void> edit(@NotNull @Positive @PathVariable Long jobId,
        @Valid @RequestBody UpdateJobCommand command) {
        command.setJobId(jobId);
        jobApplicationService.updateJob(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改定时任务状态")
    @PreAuthorize("@permission.has('system:job:changeStatus')")
    @AccessLog(title = "定时任务", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{jobId}/status")
    public ResponseDTO<Void> changeStatus(@NotNull @Positive @PathVariable Long jobId,
        @Valid @RequestBody UpdateJobStatusCommand command) {
        jobApplicationService.updateJobStatus(jobId, command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "立即运行定时任务")
    @PreAuthorize("@permission.has('system:job:run')")
    @AccessLog(title = "定时任务", businessType = BusinessTypeEnum.OTHER)
    @PostMapping("/{jobId}/run")
    public ResponseDTO<Void> run(@NotNull @Positive @PathVariable Long jobId) {
        jobApplicationService.runJobOnce(jobId);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除定时任务")
    @PreAuthorize("@permission.has('system:job:remove')")
    @AccessLog(title = "定时任务", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping
    public ResponseDTO<Void> remove(@RequestParam @NotNull @NotEmpty List<@Positive Long> jobIds) {
        jobApplicationService.deleteJobs(new BulkOperationCommand<>(jobIds));
        return ResponseDTO.ok();
    }
}
