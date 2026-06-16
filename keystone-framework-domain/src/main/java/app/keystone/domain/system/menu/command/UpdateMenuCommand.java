package app.keystone.domain.system.menu.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateMenuCommand extends AddMenuCommand {

    @NotNull
    @Positive
    private Long menuId;

}
