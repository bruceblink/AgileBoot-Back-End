package app.keystone.domain.system.post.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class AddPostCommand {

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64个字符")
    protected String postCode;

    /**
     * 岗位名称
     */
    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 64, message = "岗位名称长度不能超过64个字符")
    protected String postName;

    /**
     * 岗位排序
     */
    @NotNull(message = "显示顺序不能为空")
    protected Integer postSort;

    @Size(max = 512, message = "备注长度不能超过512个字符")
    protected String remark;

    @NotNull(message = "岗位状态不能为空")
    @Min(value = 0, message = "岗位状态值无效")
    @Max(value = 1, message = "岗位状态值无效")
    protected Integer status;

}
