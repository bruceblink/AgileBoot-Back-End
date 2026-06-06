package app.keystone.domain.system.monitor.dto;

import java.util.List;
import java.util.Properties;
import lombok.Data;


/**
 * @author likanug
 */
@Data
public class RedisCacheInfoDTO {

    private Properties info;
    private Long dbSize;
    private List<CommandStatusDTO> commandStats;

    @Data
    public static class CommandStatusDTO {
        private String name;
        private String value;
    }

}
