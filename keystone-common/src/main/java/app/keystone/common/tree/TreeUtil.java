package app.keystone.common.tree;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class TreeUtil {

    private TreeUtil() {
    }

    public static <E, T> List<Tree<T>> build(List<E> list, T rootId, BiConsumer<E, Tree<T>> nodeMapper) {
        return build(list, rootId, new TreeNodeConfig(), nodeMapper);
    }

    public static <E, T> List<Tree<T>> build(List<E> list, T rootId, TreeNodeConfig config, BiConsumer<E, Tree<T>> nodeMapper) {
        Map<T, Tree<T>> idToTree = new LinkedHashMap<>();
        List<Tree<T>> allNodes = new ArrayList<>();

        for (E item : list) {
            Tree<T> node = new Tree<>();
            nodeMapper.accept(item, node);
            allNodes.add(node);
            idToTree.put(node.getId(), node);
        }

        List<Tree<T>> roots = new ArrayList<>();
        for (Tree<T> node : allNodes) {
            T parentId = node.getParentId();
            if ((rootId == null && parentId == null) || (rootId != null && rootId.equals(parentId))) {
                roots.add(node);
                continue;
            }
            Tree<T> parent = idToTree.get(parentId);
            if (parent != null) {
                parent.addChild(node);
            } else {
                roots.add(node);
            }
        }

        return roots;
    }
}
