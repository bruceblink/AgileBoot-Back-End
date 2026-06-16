package app.keystone.domain.system.config.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * @author likanug
 */
@Data
@Schema
public class ConfigUpdateCommand {

    @Positive
    private Long configId;

    @NotNull
    @NotEmpty
    private String configValue;

}
