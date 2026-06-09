package app.keystone.domain.system.user.command;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateUserPasswordCommand {

    private Long userId;
    private String newPassword;
    private String oldPassword;

}
