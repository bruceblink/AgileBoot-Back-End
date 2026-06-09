package app.keystone.domain.system.user.command;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@NoArgsConstructor
public class UpdateUserAvatarCommand {

    public UpdateUserAvatarCommand(Long userId, String avatar) {
        this.userId = userId;
        this.avatar = avatar;
    }

    private Long userId;
    private String avatar;

}
