package app.keystone.domain.system.log.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 客户端主动写入操作日志命令。
 *
 * @author likanug
 */
@Data
@Schema(description = "客户端操作日志录入参数")
public class AddOperationLogCommand {

    @Min(value = 0, message = "业务类型值无效")
    @Max(value = 8, message = "业务类型值无效")
    @Schema(description = "业务类型（0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8清空）")
    private Integer businessType;

    @Min(value = -1, message = "请求方式值无效")
    @Max(value = 4, message = "请求方式值无效")
    @Schema(description = "请求方式（1 GET 2 POST 3 PUT 4 DELETE -1 UNKNOWN）")
    private Integer requestMethod;

    @Size(max = 64)
    @Schema(description = "请求模块")
    private String requestModule;

    @Size(max = 256)
    @Schema(description = "请求URL")
    private String requestUrl;

    @Size(max = 128)
    @Schema(description = "调用方法")
    private String calledMethod;

    @Min(value = 1, message = "操作类别值无效")
    @Max(value = 3, message = "操作类别值无效")
    @Schema(description = "操作类别（1其它 2Web用户 3手机端用户）")
    private Integer operatorType;

    @Size(max = 2048)
    @Schema(description = "请求参数")
    private String operationParam;

    @Size(max = 2048)
    @Schema(description = "返回参数")
    private String operationResult;

    @Min(value = 0, message = "操作状态值无效")
    @Max(value = 1, message = "操作状态值无效")
    @Schema(description = "操作状态（1正常 0异常）")
    private Integer status;

    @Size(max = 2048)
    @Schema(description = "错误消息")
    private String errorStack;

    @Size(max = 40)
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$")
    @Schema(description = "操作时间，UTC时间，格式：yyyy-MM-dd HH:mm:ss，不传则使用服务端当前时间")
    private String operationTime;

}
