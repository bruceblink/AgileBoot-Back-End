package app.keystone.infrastructure.mybatisplus;

import java.util.Arrays;

/**
 * MySQL compatible helper functions used by mapper SQL.
 *
 * @author likanug
 */
public class MySqlFunction {

    private MySqlFunction() {
    }

    public static boolean findInSet(String target, String setString) {
        if (setString == null) {
            return false;
        }

        return Arrays.asList(setString.split(",")).contains(target);
    }

}
