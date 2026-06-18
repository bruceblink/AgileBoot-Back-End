package app.keystone.domain.system.job.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Candidate Spring bean method that can be used as a scheduled job invoke target.
 *
 * @author likanug
 */
@Data
@AllArgsConstructor
public class JobInvokeTargetDTO {

    private String invokeTarget;
    private String beanName;
    private String methodName;
    private String name;
    private String group;
    private String description;
}
