package app.keystone.admin.controller.system;

import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.domain.system.serviceclient.ServiceClientApplicationService;
import app.keystone.domain.system.serviceclient.command.RegisterServiceClientCommand;
import app.keystone.domain.system.serviceclient.command.UpdateServiceClientCommand;
import app.keystone.domain.system.serviceclient.dto.ServiceClientDTO;
import app.keystone.domain.system.serviceclient.query.ServiceClientQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "服务客户端API", description = "Keylo服务客户端注册")
@RestController
@RequestMapping("/system/service-clients")
@RequiredArgsConstructor
public class SysServiceClientController extends BaseController {

    private final ServiceClientApplicationService serviceClientApplicationService;

    @Operation(summary = "服务客户端列表", description = "分页查询Keylo服务客户端")
    @PreAuthorize("@permission.has('system:user:list')")
    @GetMapping
    public ResponseDTO<PageDTO<ServiceClientDTO>> list(ServiceClientQuery query) {
        return ResponseDTO.ok(serviceClientApplicationService.list(query));
    }

    @Operation(summary = "服务客户端详情", description = "查询Keylo服务客户端详情")
    @PreAuthorize("@permission.has('system:user:query')")
    @GetMapping("/{serviceId}")
    public ResponseDTO<ServiceClientDTO> detail(@PathVariable String serviceId) {
        return ResponseDTO.ok(serviceClientApplicationService.detail(serviceId));
    }

    @Operation(summary = "新增服务客户端", description = "注册Keylo服务客户端")
    @PreAuthorize("@permission.has('system:user:add')")
    @AccessLog(title = "服务客户端管理", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<ServiceClientDTO> add(@Valid @RequestBody RegisterServiceClientCommand command) {
        return ResponseDTO.ok(serviceClientApplicationService.register(command));
    }

    @Operation(summary = "修改服务客户端", description = "修改Keylo服务客户端配置")
    @PreAuthorize("@permission.has('system:user:edit')")
    @AccessLog(title = "服务客户端管理", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{serviceId}")
    public ResponseDTO<ServiceClientDTO> edit(@PathVariable String serviceId,
        @Valid @RequestBody UpdateServiceClientCommand command) {
        return ResponseDTO.ok(serviceClientApplicationService.update(serviceId, command));
    }
}
