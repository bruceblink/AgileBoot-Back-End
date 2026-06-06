package app.keystone.domain.system.user.command;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public class UpdateProfileCommand {

    private Long userId;

    private Integer sex;
    private String nickName;
    private String phoneNumber;
    private String email;

}
