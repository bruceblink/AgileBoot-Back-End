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
 * 字典数据表
 * @author likanug
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
@ApiModel(value = "SysDictDataEntity对象", description = "字典数据表")
public class SysDictDataEntity extends BaseEntity<SysDictDataEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("字典编码")
    @TableId(value = "dict_code", type = IdType.AUTO)
    private Long dictCode;

    @ApiModelProperty("字典类型")
    @TableField("dict_type")
    private String dictType;

    @ApiModelProperty("字典标签")
    @TableField("dict_label")
    private String dictLabel;

    @ApiModelProperty("字典键值")
    @TableField("dict_value")
    private String dictValue;

    @ApiModelProperty("字典排序")
    @TableField("dict_sort")
    private Integer dictSort;

    @ApiModelProperty("是否默认（1是 0否）")
    @TableField("is_default")
    private Integer isDefault;

    @ApiModelProperty("样式属性")
    @TableField("css_class")
    private String cssClass;

    @ApiModelProperty("表格回显样式")
    @TableField("list_class")
    private String listClass;

    @ApiModelProperty("状态（1正常 0停用）")
    @TableField("status")
    private Integer status;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.dictCode;
    }
}
