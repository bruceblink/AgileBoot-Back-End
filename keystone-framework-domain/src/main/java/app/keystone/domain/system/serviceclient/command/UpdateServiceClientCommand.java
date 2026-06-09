package app.keystone.domain.system.serviceclient.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class UpdateServiceClientCommand {

    @NotBlank(message = "服务客户端名称不能为空")
    @Size(max = 128, message = "服务客户端名称长度不能超过128个字符")
    private String name;

    @Size(max = 512, message = "服务客户端描述长度不能超过512个字符")
    private String description;

    @NotEmpty(message = "服务客户端Scope不能为空")
    private List<@NotBlank(message = "服务客户端Scope不能为空") String> allowedScopes = new ArrayList<>();

    @NotEmpty(message = "服务客户端Audience不能为空")
    private List<@NotBlank(message = "服务客户端Audience不能为空") String> allowedAudiences = new ArrayList<>();

    private Boolean active;

    @Size(max = 64, message = "集成类型长度不能超过64个字符")
    private String integrationType = "internal";

    private Boolean introspectionAllowed = true;

    @Positive(message = "Token TTL必须大于0")
    private Long tokenTtlSeconds;

    @Size(max = 128, message = "负责人长度不能超过128个字符")
    private String owner;

    @Size(max = 128, message = "联系方式长度不能超过128个字符")
    private String contact;
}
