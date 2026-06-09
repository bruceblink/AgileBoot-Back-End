package app.keystone.domain.system.menu.dto;

import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.domain.system.menu.db.SysMenuEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @author likanug
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuDetailDTO extends MenuDTO {

    public MenuDetailDTO(SysMenuEntity entity) {
        super(entity);
        if (entity == null) {
            return;
        }
        if (StringUtils.isNotEmpty(entity.getMetaInfo()) && JacksonUtil.isJson(entity.getMetaInfo())) {
            this.meta = JacksonUtil.from(entity.getMetaInfo(), MetaDTO.class);
        }
        this.permission = entity.getPermission();
    }

    private String permission;
    private MetaDTO meta;

}
