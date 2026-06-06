package app.keystone.domain.system.role.command;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@NoArgsConstructor
public class UpdateStatusCommand {

    private Long roleId;

    private Integer status;

}
