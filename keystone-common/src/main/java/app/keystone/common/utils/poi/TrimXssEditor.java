package app.keystone.common.utils.poi;

/**
 * @author likanug
 * 读取excel的时候，去除掉html相关的标签  避免xss注入
 */
public class TrimXssEditor {

    private TrimXssEditor() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "");
    }
}
