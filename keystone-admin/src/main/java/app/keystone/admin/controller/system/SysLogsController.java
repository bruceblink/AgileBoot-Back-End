package app.keystone.admin.controller.system;


import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.common.utils.poi.CustomExcelUtil;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.system.log.LogApplicationService;
import app.keystone.domain.system.log.command.AddOperationLogCommand;
import app.keystone.domain.system.log.dto.LoginLogDTO;
import app.keystone.domain.system.log.query.LoginLogQuery;
import app.keystone.domain.system.log.dto.OperationLogDTO;
import app.keystone.domain.system.log.query.OperationLogQuery;
import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统访问记录
 *
 * @author valarchie
 */
@Tag(name = "日志API", description = "日志相关API")
@RestController
@RequestMapping("/logs")
@Validated
@RequiredArgsConstructor
public class SysLogsController extends BaseController {

    private final LogApplicationService logApplicationService;

    @Operation(summary = "登录日志列表")
    @PreAuthorize("@permission.has('monitor:logininfor:list')")
    @GetMapping("/loginLogs")
    public ResponseDTO<PageDTO<LoginLogDTO>> loginInfoList(LoginLogQuery query) {
        PageDTO<LoginLogDTO> pageDTO = logApplicationService.getLoginInfoList(query);
        return ResponseDTO.ok(pageDTO);
    }

    @Operation(summary = "登录日志导出", description = "将登录日志导出到excel")
    @AccessLog(title = "登录日志", businessType = BusinessTypeEnum.EXPORT)
    @PreAuthorize("@permission.has('monitor:logininfor:export')")
    @GetMapping("/loginLogs/excel")
    public void loginInfosExcel(HttpServletResponse response, LoginLogQuery query) {
        PageDTO<LoginLogDTO> pageDTO = logApplicationService.getLoginInfoList(query);
        CustomExcelUtil.writeToResponse(pageDTO.getRows(), LoginLogDTO.class, response);
    }

    @Operation(summary = "删除登录日志")
    @PreAuthorize("@permission.has('monitor:logininfor:remove')")
    @AccessLog(title = "登录日志", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping("/loginLogs")
    public ResponseDTO<Void> removeLoginInfos(@RequestParam @NotNull @NotEmpty List<Long> ids) {
        logApplicationService.deleteLoginInfo(new BulkOperationCommand<>(ids));
        return ResponseDTO.ok();
    }

    @Operation(summary = "操作日志列表")
    @PreAuthorize("@permission.has('monitor:operlog:list')")
    @GetMapping("/operationLogs")
    public ResponseDTO<PageDTO<OperationLogDTO>> operationLogs(OperationLogQuery query) {
        PageDTO<OperationLogDTO> pageDTO = logApplicationService.getOperationLogList(query);
        return ResponseDTO.ok(pageDTO);
    }

    @Operation(summary = "新增操作日志", description = "客户端主动写入操作日志")
    @PostMapping("/operationLogs")
    public ResponseDTO<Void> addOperationLog(@Valid @RequestBody AddOperationLogCommand command,
        HttpServletRequest request) {
        SystemLoginUser loginUser = AuthenticationUtils.getSystemLoginUser();
        logApplicationService.addOperationLog(command, loginUser, request.getRemoteAddr(), request.getRequestURI(),
            request.getMethod());
        return ResponseDTO.ok();
    }

//    @GetMapping("/download")
//    public ResponseEntity<InputStreamResource> downloadFile() throws IOException {
//        // 从文件系统或其他位置获取文件输入流
//        File file = new File("path/to/file");
//        InputStream inputStream = new FileInputStream(file);
//        CustomExcelUtil.wri
//
//        // 创建一个 InputStreamResource 对象，将文件输入流包装在其中
//        InputStreamResource resource = new InputStreamResource(inputStream);
//
//        // 返回 ResponseEntity 对象，其中包含 InputStreamResource 对象和文件名
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .contentLength(file.length())
//                .body(resource);
//    }
    /**
     * @param response
     * @param query
     */
    @Operation(summary = "操作日志导出")
    @AccessLog(title = "操作日志", businessType = BusinessTypeEnum.EXPORT)
    @PreAuthorize("@permission.has('monitor:operlog:export')")
    @GetMapping("/operationLogs/excel")
    public void operationLogsExcel(HttpServletResponse response, OperationLogQuery query) {
        PageDTO<OperationLogDTO> pageDTO = logApplicationService.getOperationLogList(query);
        CustomExcelUtil.writeToResponse(pageDTO.getRows(), OperationLogDTO.class, response);
    }

    @Operation(summary = "删除操作日志")
    @AccessLog(title = "操作日志", businessType = BusinessTypeEnum.DELETE)
    @PreAuthorize("@permission.has('monitor:operlog:remove')")
    @DeleteMapping("/operationLogs")
    public ResponseDTO<Void> removeOperationLogs(@RequestParam List<Long> operationIds) {
        logApplicationService.deleteOperationLog(new BulkOperationCommand<>(operationIds));
        return ResponseDTO.ok();
    }


}
