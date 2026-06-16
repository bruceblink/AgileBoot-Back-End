package app.keystone.domain.system.post.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdatePostCommand extends AddPostCommand {

    @NotNull(message = "岗位ID不能为空")
    @Positive(message = "岗位ID必须为正数")
    private Long postId;

}
