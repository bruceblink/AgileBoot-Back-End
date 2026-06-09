package app.keystone.domain.system.dept.dto;

import app.keystone.common.enums.common.StatusEnum;
import app.keystone.common.enums.BasicEnumUtil;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import java.util.Date;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class DeptDTO {

    public DeptDTO(SysDeptEntity entity) {
        if (entity != null) {
            this.id = entity.getDeptId();
            this.parentId = entity.getParentId();
            this.deptName = entity.getDeptName();
            this.orderNum = entity.getOrderNum();
            this.leaderName = entity.getLeaderName();
            this.email = entity.getEmail();
            this.phone = entity.getPhone();
            this.status = entity.getStatus();
            this.createTime = entity.getCreateTime();
            this.statusStr = BasicEnumUtil.getDescriptionByValue(StatusEnum.class, entity.getStatus());
        }
    }


    private Long id;

    private Long parentId;

    private String deptName;

    private Integer orderNum;

    private String leaderName;

    private String phone;

    private String email;

    private Integer status;

    private String statusStr;

    private Date createTime;

}
