package app.keystone.admin.controller.system;

import app.keystone.common.tree.Tree;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.domain.system.dept.DeptApplicationService;
import app.keystone.domain.system.dept.command.AddDeptCommand;
import app.keystone.domain.system.dept.command.UpdateDeptCommand;
import app.keystone.domain.system.dept.dto.DeptDTO;
import app.keystone.domain.system.dept.query.DeptQuery;
import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.enums.common.BusinessTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
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
 * 部门信息
 *
 * @author likanug
 */
@RestController
@RequestMapping("/system")
@Validated
@RequiredArgsConstructor
@Tag(name = "部门API", description = "部门相关的增删查改")
public class SysDeptController extends BaseController {

    private final DeptApplicationService deptApplicationService;

    /**
     * 获取部门列表
     */
    @Operation(summary = "部门列表")
    @PreAuthorize("@permission.has('system:dept:list')")
    @GetMapping("/depts")
    public ResponseDTO<List<DeptDTO>> list(DeptQuery query) {
        List<DeptDTO> deptList = deptApplicationService.getDeptList(query);
        return ResponseDTO.ok(deptList);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @Operation(summary = "部门详情")
    @PreAuthorize("@permission.has('system:dept:query')")
    @GetMapping(value = "/dept/{deptId}")
    public ResponseDTO<DeptDTO> getInfo(@PathVariable @NotNull @Positive Long deptId) {
        DeptDTO dept = deptApplicationService.getDeptInfo(deptId);
        return ResponseDTO.ok(dept);
    }

    /**
     * 获取部门下拉树列表
     */
    @Operation(summary = "获取部门树级结构")
    @GetMapping("/depts/dropdown")
    public ResponseDTO<List<Tree<Long>>> dropdownList() {
        List<Tree<Long>> deptTree = deptApplicationService.getDeptTree();
        return ResponseDTO.ok(deptTree);
    }

    /**
     * 新增部门
     */
    @Operation(summary = "新增部门")
    @PreAuthorize("@permission.has('system:dept:add')")
    @AccessLog(title = "部门管理", businessType = BusinessTypeEnum.ADD)
    @PostMapping("/dept")
    public ResponseDTO<Void> add(@Valid @RequestBody AddDeptCommand addCommand) {
        deptApplicationService.addDept(addCommand);
        return ResponseDTO.ok();
    }

    /**
     * 修改部门
     */
    @Operation(summary = "修改部门")
    @PreAuthorize("@permission.has('system:dept:edit') AND @dataScope.checkDeptId(#updateCommand.deptId)")
    @AccessLog(title = "部门管理", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/dept/{deptId}")
    public ResponseDTO<Void> edit(@PathVariable("deptId") @NotNull @Positive Long deptId,
        @Valid @RequestBody UpdateDeptCommand updateCommand) {
        updateCommand.setDeptId(deptId);
        deptApplicationService.updateDept(updateCommand);
        return ResponseDTO.ok();
    }

    /**
     * 删除部门
     */
    @Operation(summary = "删除部门")
    @PreAuthorize("@permission.has('system:dept:remove') AND @dataScope.checkDeptId(#deptId)")
    @AccessLog(title = "部门管理", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping("/dept/{deptId}")
    public ResponseDTO<Void> remove(@PathVariable @NotNull @Positive Long deptId) {
        deptApplicationService.removeDept(deptId);
        return ResponseDTO.ok();
    }
}
