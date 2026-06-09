package app.keystone.admin.customize.service.login.command;

import lombok.Data;

/**
 * Keylo token 登录请求
 */
@Data
public class KeyloLoginCommand {

    /**
     * Keylo 分发的 access token
     */
    private String accessToken;

    /**
     * 是否接管旧会话
     */
    private Boolean forceLogin;
}
