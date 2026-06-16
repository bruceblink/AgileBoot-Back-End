package app.keystone.domain.system.menu.command;

import app.keystone.domain.system.menu.dto.MetaDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class AddMenuCommand {

    @NotNull(message = "父级菜单ID不能为空")
    @PositiveOrZero(message = "父级菜单ID不能小于0")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String menuName;
    /**
     * 路由名称 必须唯一
     */
    private String routerName;

    @Size(max = 200, message = "路由地址不能超过200个字符")
    private String path;

    @NotNull(message = "菜单状态不能为空")
    @Min(value = 0, message = "菜单状态值无效")
    @Max(value = 1, message = "菜单状态值无效")
    private Integer status;

    @NotNull(message = "菜单类型不能为空")
    @Min(value = 1, message = "菜单类型值无效")
    @Max(value = 4, message = "菜单类型值无效")
    private Integer menuType;

    @NotNull(message = "菜单按钮标识不能为空")
    private Boolean isButton;

    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String permission;

    @Valid
    private MetaDTO meta;

}
