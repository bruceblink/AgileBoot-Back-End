package app.keystone.domain.system.dict.dto;

import app.keystone.domain.system.dict.db.SysDictDataEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * 字典数据 DTO
 * @author likanug
 */
@Data
@Schema(name = "DictDataDTO", description = "字典数据信息")
public class DictDataDTO {

    public DictDataDTO(SysDictDataEntity entity) {
        if (entity != null) {
            dictCode = entity.getDictCode();
            dictType = entity.getDictType();
            dictLabel = entity.getDictLabel();
            dictValue = entity.getDictValue();
            dictSort = entity.getDictSort();
            isDefault = entity.getIsDefault();
            cssClass = entity.getCssClass();
            listClass = entity.getListClass();
            status = entity.getStatus();
            remark = entity.getRemark();
            createTime = entity.getCreateTime();
        }
    }

    private Long dictCode;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private Integer isDefault;
    private String cssClass;
    private String listClass;
    private Integer status;
    private String remark;
    private Date createTime;
}
