package app.keystone.admin.controller.system;

import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.common.utils.poi.CustomExcelUtil;
import app.keystone.domain.system.role.RoleApplicationService;
import app.keystone.domain.system.role.command.AddRoleCommand;
import app.keystone.domain.system.role.command.UpdateDataScopeCommand;
import app.keystone.domain.system.role.command.UpdateRoleCommand;
import app.keystone.domain.system.role.command.UpdateStatusCommand;
import app.keystone.domain.system.role.dto.RoleDTO;
import app.keystone.domain.system.role.query.AllocatedRoleQuery;
import app.keystone.domain.system.role.query.RoleQuery;
import app.keystone.domain.system.role.query.UnallocatedRoleQuery;
import app.keystone.domain.system.user.dto.UserDTO;
import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.enums.common.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色信息
 *
 * @author likanug
 */
@Tag(name = "角色API", description = "角色相关的增删查改")
@RestController
@RequestMapping("/system/role")
@Validated
@RequiredArgsConstructor
public class SysRoleController extends BaseController {

    private final RoleApplicationService roleApplicationService;

    @Operation(summary = "角色列表")
    @PreAuthorize("@permission.has('system:role:list')")
    @GetMapping("/list")
    public ResponseDTO<PageDTO<RoleDTO>> list(@Validated RoleQuery query) {
        PageDTO<RoleDTO> pageDTO = roleApplicationService.getRoleList(query);
        return ResponseDTO.ok(pageDTO);
    }

    @Operation(summary = "角色列表导出")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.EXPORT)
    @PreAuthorize("@permission.has('system:role:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, @Validated RoleQuery query) {
        CustomExcelUtil.writeToResponse(roleApplicationService.exportRoles(query), RoleDTO.class, response);
    }

    /**
     * 根据角色编号获取详细信息
     */
    @Operation(summary = "角色详情")
    @PreAuthorize("@permission.has('system:role:query')")
    @GetMapping(value = "/{roleId}")
    public ResponseDTO<RoleDTO> getInfo(@PathVariable("roleId") @NotNull @Positive Long roleId) {
        RoleDTO roleInfo = roleApplicationService.getRoleInfo(roleId);
        return ResponseDTO.ok(roleInfo);
    }

    /**
     * 新增角色
     */
    @Operation(summary = "添加角色")
    @PreAuthorize("@permission.has('system:role:add')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<Void> add(@Validated @RequestBody AddRoleCommand addCommand) {
        roleApplicationService.addRole(addCommand);
        return ResponseDTO.ok();
    }

    /**
     * 移除角色
     */
    @Operation(summary = "删除角色")
    @PreAuthorize("@permission.has('system:role:remove')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping(value = "/{roleId}")
    public ResponseDTO<Void> remove(@PathVariable("roleId") @NotNull @NotEmpty List<@Positive Long> roleIds) {
        roleApplicationService.deleteRoleByBulk(roleIds);
        return ResponseDTO.ok();
    }

    /**
     * 修改保存角色
     */
    @Operation(summary = "修改角色")
    @PreAuthorize("@permission.has('system:role:edit')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping
    public ResponseDTO<Void> edit(@Validated @RequestBody UpdateRoleCommand updateCommand) {
        roleApplicationService.updateRole(updateCommand);
        return ResponseDTO.ok();
    }

    /**
     * 修改保存数据权限
     */
    @Operation(summary = "修改角色数据权限")
    @PreAuthorize("@permission.has('system:role:edit')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{roleId}/dataScope")
    public ResponseDTO<Void> dataScope(@PathVariable("roleId") @NotNull @Positive Long roleId,
        @Valid @RequestBody UpdateDataScopeCommand command) {
        command.setRoleId(roleId);

        roleApplicationService.updateDataScope(command);
        return ResponseDTO.ok();
    }

    /**
     * 角色状态修改
     */
    @Operation(summary = "修改角色状态")
    @PreAuthorize("@permission.has('system:role:edit')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{roleId}/status")
    public ResponseDTO<Void> changeStatus(@PathVariable("roleId") @NotNull @Positive Long roleId,
        @Valid @RequestBody UpdateStatusCommand command) {
        command.setRoleId(roleId);

        roleApplicationService.updateStatus(command);
        return ResponseDTO.ok();
    }


    /**
     * 查询已分配用户角色列表
     */
    @Operation(summary = "已关联该角色的用户列表")
    @PreAuthorize("@permission.has('system:role:list')")
    @GetMapping("/{roleId}/allocated/list")
    public ResponseDTO<PageDTO<UserDTO>> allocatedUserList(@PathVariable("roleId") @NotNull @Positive Long roleId,
        @Validated AllocatedRoleQuery query) {
        query.setRoleId(roleId);
        PageDTO<UserDTO> page = roleApplicationService.getAllocatedUserList(query);
        return ResponseDTO.ok(page);
    }

    /**
     * 查询未分配用户角色列表
     */
    @Operation(summary = "未关联该角色的用户列表")
    @PreAuthorize("@permission.has('system:role:list')")
    @GetMapping("/{roleId}/unallocated/list")
    public ResponseDTO<PageDTO<UserDTO>> unallocatedUserList(@PathVariable("roleId") @NotNull @Positive Long roleId,
        @Validated UnallocatedRoleQuery query) {
        query.setRoleId(roleId);
        PageDTO<UserDTO> page = roleApplicationService.getUnallocatedUserList(query);
        return ResponseDTO.ok(page);
    }


    /**
     * 批量取消授权用户
     */
    @Operation(summary = "批量解除角色和用户的关联")
    @PreAuthorize("@permission.has('system:role:edit')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.GRANT)
    @DeleteMapping("/users/{userIds}/grant/bulk")
    public ResponseDTO<Void> deleteRoleOfUserByBulk(
        @PathVariable("userIds") @NotNull @NotEmpty List<@Positive Long> userIds) {
        roleApplicationService.deleteRoleOfUserByBulk(userIds);
        return ResponseDTO.ok();
    }

    /**
     * 批量选择用户授权
     */
    @Operation(summary = "批量添加用户和角色关联")
    @PreAuthorize("@permission.has('system:role:edit')")
    @AccessLog(title = "角色管理", businessType = BusinessTypeEnum.GRANT)
    @PostMapping("/{roleId}/users/{userIds}/grant/bulk")
    public ResponseDTO<Void> addRoleForUserByBulk(@PathVariable("roleId") @NotNull @Positive Long roleId,
        @PathVariable("userIds") @NotNull @NotEmpty List<@Positive Long> userIds) {
        roleApplicationService.addRoleOfUserByBulk(roleId, userIds);
        return ResponseDTO.ok();
    }

}
