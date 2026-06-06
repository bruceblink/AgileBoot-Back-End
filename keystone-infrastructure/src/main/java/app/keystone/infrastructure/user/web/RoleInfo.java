package app.keystone.infrastructure.user.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.SetUtils;

/**
 * @author likanug
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final RoleInfo EMPTY_ROLE = new RoleInfo();
    public static final long ADMIN_ROLE_ID = -1;
    public static final String ADMIN_ROLE_KEY = "admin";
    public static final String ALL_PERMISSIONS = "*:*:*";

    public static final Set<String> ADMIN_PERMISSIONS = SetUtils.hashSet(ALL_PERMISSIONS);


    public RoleInfo(Long roleId, String roleKey, DataScopeEnum dataScope, Set<Long> deptIdSet,
        Set<String> menuPermissions, Set<Long> menuIds) {
        this.roleId = roleId;
        this.roleKey = roleKey;
        this.dataScope = dataScope;
        this.deptIdSet = deptIdSet == null ? SetUtils.emptySet() : new HashSet<>(deptIdSet);
        this.menuPermissions = menuPermissions == null ? SetUtils.emptySet() : new HashSet<>(menuPermissions);
        this.menuIds = menuIds == null ? SetUtils.emptySet() : new HashSet<>(menuIds);
    }

    public RoleInfo(RoleInfo other) {
        this(
            other == null ? null : other.roleId,
            other == null ? null : other.roleKey,
            other == null ? null : other.dataScope,
            other == null ? null : other.deptIdSet,
            other == null ? null : other.menuPermissions,
            other == null ? null : other.menuIds
        );
        this.roleName = other == null ? null : other.roleName;
    }


    private Long roleId;
    private String roleName;
    private DataScopeEnum dataScope;
    private Set<Long> deptIdSet;
    private String roleKey;
    private Set<String> menuPermissions;
    private Set<Long> menuIds;

}
