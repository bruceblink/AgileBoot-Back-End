package app.keystone.domain.system.user.dto;

import app.keystone.domain.system.role.dto.RoleDTO;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UserInfoDTO {

    private UserDTO user;
    private RoleDTO role;

}
