package app.keystone.infrastructure.mybatisplus;

import java.util.Arrays;

/**
 * 由于H2不支持大部分Mysql的函数  所以要自己实现
 * 在H2的初始化 h2sql/keystone_schema.sql加上这句
 * CREATE ALIAS FIND_IN_SET FOR "app.keystone.infrastructure.mybatisplus.MySqlFunction.find_in_set";
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
