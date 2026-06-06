package app.keystone.common.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tree<T> {

    private T id;
    private T parentId;
    private final Map<String, Object> extra = new HashMap<>();
    private List<Tree<T>> children = new ArrayList<>();

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    public T getParentId() {
        return parentId;
    }

    public void setParentId(T parentId) {
        this.parentId = parentId;
    }

    public Object get(String key) {
        return extra.get(key);
    }

    public void putExtra(String key, Object value) {
        extra.put(key, value);
    }

    public List<Tree<T>> getChildren() {
        return List.copyOf(children);
    }

    public void setChildren(List<Tree<T>> children) {
        this.children = children == null ? new ArrayList<>() : new ArrayList<>(children);
    }

    public void addChild(Tree<T> child) {
        if (child != null) {
            children.add(child);
        }
    }
}
