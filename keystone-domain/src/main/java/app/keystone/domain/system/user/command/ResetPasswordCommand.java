package app.keystone.domain.system.user.command;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public class ResetPasswordCommand {

    private Long userId;
    private String password;

}
