package app.keystone.admin.controller.system;

import app.keystone.admin.customize.aop.accessLog.AccessLog;
import app.keystone.common.core.base.BaseController;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.domain.system.dict.DictApplicationService;
import app.keystone.domain.system.dict.command.AddDictDataCommand;
import app.keystone.domain.system.dict.command.AddDictTypeCommand;
import app.keystone.domain.system.dict.command.UpdateDictDataCommand;
import app.keystone.domain.system.dict.command.UpdateDictTypeCommand;
import app.keystone.domain.system.dict.dto.DictDataDTO;
import app.keystone.domain.system.dict.dto.DictTypeDTO;
import app.keystone.domain.system.dict.query.DictDataQuery;
import app.keystone.domain.system.dict.query.DictTypeQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据字典 增删查改
 * @author likanug
 */
@RestController
@RequestMapping("/system")
@Validated
@RequiredArgsConstructor
@Tag(name = "字典API", description = "字典类型与字典数据的增删查改")
public class SysDictController extends BaseController {

    private final DictApplicationService dictApplicationService;

    // ======================== 字典类型 ========================

    @Operation(summary = "字典类型列表", description = "分页获取字典类型列表")
    @PreAuthorize("@permission.has('system:dict:list')")
    @GetMapping("/dict/types")
    public ResponseDTO<PageDTO<DictTypeDTO>> listDictTypes(DictTypeQuery query) {
        return ResponseDTO.ok(dictApplicationService.getDictTypeList(query));
    }

    @Operation(summary = "字典类型详情")
    @PreAuthorize("@permission.has('system:dict:query')")
    @GetMapping("/dict/type/{dictId}")
    public ResponseDTO<DictTypeDTO> getDictType(@NotNull @Positive @PathVariable Long dictId) {
        return ResponseDTO.ok(dictApplicationService.getDictTypeInfo(dictId));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("@permission.has('system:dict:add')")
    @AccessLog(title = "字典类型", businessType = BusinessTypeEnum.ADD)
    @PostMapping("/dict/type")
    public ResponseDTO<Void> addDictType(@Valid @RequestBody AddDictTypeCommand command) {
        dictApplicationService.addDictType(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改字典类型")
    @PreAuthorize("@permission.has('system:dict:edit')")
    @AccessLog(title = "字典类型", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/dict/type/{dictId}")
    public ResponseDTO<Void> updateDictType(@NotNull @Positive @PathVariable Long dictId,
        @Valid @RequestBody UpdateDictTypeCommand command) {
        command.setDictId(dictId);
        dictApplicationService.updateDictType(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除字典类型")
    @PreAuthorize("@permission.has('system:dict:remove')")
    @AccessLog(title = "字典类型", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping("/dict/type/{dictId}")
    public ResponseDTO<Void> deleteDictType(@NotNull @Positive @PathVariable Long dictId) {
        dictApplicationService.deleteDictType(dictId);
        return ResponseDTO.ok();
    }

    // ======================== 字典数据 ========================

    @Operation(summary = "字典数据列表", description = "分页获取字典数据列表")
    @PreAuthorize("@permission.has('system:dict:list')")
    @GetMapping("/dict/data/list")
    public ResponseDTO<PageDTO<DictDataDTO>> listDictData(DictDataQuery query) {
        return ResponseDTO.ok(dictApplicationService.getDictDataList(query));
    }

    @Operation(summary = "根据字典类型获取字典数据", description = "供前端下拉框使用")
    @GetMapping("/dict/data/type/{dictType}")
    public ResponseDTO<List<DictDataDTO>> getDictDataByType(@PathVariable String dictType) {
        return ResponseDTO.ok(dictApplicationService.getDictDataByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @PreAuthorize("@permission.has('system:dict:query')")
    @GetMapping("/dict/data/{dictCode}")
    public ResponseDTO<DictDataDTO> getDictData(@NotNull @Positive @PathVariable Long dictCode) {
        return ResponseDTO.ok(dictApplicationService.getDictDataInfo(dictCode));
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("@permission.has('system:dict:add')")
    @AccessLog(title = "字典数据", businessType = BusinessTypeEnum.ADD)
    @PostMapping("/dict/data")
    public ResponseDTO<Void> addDictData(@Valid @RequestBody AddDictDataCommand command) {
        dictApplicationService.addDictData(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改字典数据")
    @PreAuthorize("@permission.has('system:dict:edit')")
    @AccessLog(title = "字典数据", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/dict/data/{dictCode}")
    public ResponseDTO<Void> updateDictData(@NotNull @Positive @PathVariable Long dictCode,
        @Valid @RequestBody UpdateDictDataCommand command) {
        command.setDictCode(dictCode);
        dictApplicationService.updateDictData(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除字典数据")
    @PreAuthorize("@permission.has('system:dict:remove')")
    @AccessLog(title = "字典数据", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping("/dict/data/{dictCode}")
    public ResponseDTO<Void> deleteDictData(@NotNull @Positive @PathVariable Long dictCode) {
        dictApplicationService.deleteDictData(dictCode);
        return ResponseDTO.ok();
    }
}
