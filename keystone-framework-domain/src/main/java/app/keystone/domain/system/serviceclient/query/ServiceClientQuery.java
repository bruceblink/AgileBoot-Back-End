package app.keystone.domain.system.serviceclient.query;

import app.keystone.common.core.page.AbstractPageQuery;
import app.keystone.domain.system.serviceclient.dto.ServiceClientDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceClientQuery extends AbstractPageQuery<ServiceClientDTO> {

    private String serviceId;

    private String name;

    private Boolean active;

    private String integrationType;

    @Override
    public QueryWrapper<ServiceClientDTO> addQueryCondition() {
        return new QueryWrapper<>();
    }
}
