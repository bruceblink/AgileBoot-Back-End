package app.keystone.common.utils.sql;

/**
 * Utilities for values used in SQL LIKE expressions.
 */
public final class SqlLikeUtils {

    private SqlLikeUtils() {
    }

    /**
     * Escape LIKE wildcards so user input is matched literally.
     *
     * @param value raw search text
     * @return escaped search text, or {@code null} when the input is null
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\' || ch == '%' || ch == '_') {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
    }
}
