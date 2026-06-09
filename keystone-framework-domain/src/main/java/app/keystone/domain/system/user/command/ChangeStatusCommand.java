package app.keystone.domain.system.user.command;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public class ChangeStatusCommand {

    private Long userId;
    private String status;

}
