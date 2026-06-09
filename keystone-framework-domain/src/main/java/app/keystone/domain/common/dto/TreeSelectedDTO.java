package app.keystone.domain.common.dto;

import app.keystone.common.tree.Tree;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author likanug
 */
@Data
@NoArgsConstructor
public class TreeSelectedDTO {

    private List<Long> checkedKeys;
    private List<Tree<Long>> menus;
    private List<Tree<Long>> depts;

}
