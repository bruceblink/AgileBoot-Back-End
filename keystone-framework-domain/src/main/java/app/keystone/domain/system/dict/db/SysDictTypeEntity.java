package app.keystone.domain.system.dict.db;

import app.keystone.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典类型表
 * @author likanug
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
@ApiModel(value = "SysDictTypeEntity对象", description = "字典类型表")
public class SysDictTypeEntity extends BaseEntity<SysDictTypeEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("字典主键")
    @TableId(value = "dict_id", type = IdType.AUTO)
    private Long dictId;

    @ApiModelProperty("字典名称")
    @TableField("dict_name")
    private String dictName;

    @ApiModelProperty("字典类型")
    @TableField("dict_type")
    private String dictType;

    @ApiModelProperty("状态（1正常 0停用）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.dictId;
    }
}
