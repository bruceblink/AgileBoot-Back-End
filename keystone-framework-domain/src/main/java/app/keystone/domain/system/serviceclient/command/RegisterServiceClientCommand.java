package app.keystone.domain.system.serviceclient.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterServiceClientCommand extends UpdateServiceClientCommand {

    @NotBlank(message = "服务客户端ID不能为空")
    @Size(max = 128, message = "服务客户端ID长度不能超过128个字符")
    private String serviceId;

    @NotBlank(message = "服务客户端密钥不能为空")
    @Size(max = 256, message = "服务客户端密钥长度不能超过256个字符")
    private String serviceSecret;
}
