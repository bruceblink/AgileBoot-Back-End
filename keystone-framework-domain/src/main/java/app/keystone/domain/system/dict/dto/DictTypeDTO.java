package app.keystone.domain.system.dict.dto;

import app.keystone.domain.system.dict.db.SysDictTypeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 字典类型 DTO
 * @author likanug
 */
@Data
@Schema(name = "DictTypeDTO", description = "字典类型信息")
public class DictTypeDTO {

    public DictTypeDTO(SysDictTypeEntity entity) {
        if (entity != null) {
            dictId = entity.getDictId();
            dictName = entity.getDictName();
            dictType = entity.getDictType();
            status = entity.getStatus();
            remark = entity.getRemark();
            createTime = entity.getCreateTime();
        }
    }

    private Long dictId;
    private String dictName;
    private String dictType;
    private Integer status;
    private String remark;
    private Date createTime;
}
