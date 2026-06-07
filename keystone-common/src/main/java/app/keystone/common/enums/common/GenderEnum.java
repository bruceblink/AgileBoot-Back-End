package app.keystone.common.enums.common;

import app.keystone.common.enums.dictionary.CssTag;
import app.keystone.common.enums.DictionaryEnum;

/**
 * 对应sys_user的sex字段
 *
 * @author likanug
 */
public enum GenderEnum implements DictionaryEnum<Integer> {

    /**
     * 用户性别
     */
    FEMALE(0, "女", CssTag.PRIMARY),
    MALE(1, "男", CssTag.PRIMARY),
    UNKNOWN(2, "未知", CssTag.PRIMARY);

    private final int value;
    private final String description;
    private final String cssTag;

    GenderEnum(int value, String description, String cssTag) {
        this.value = value;
        this.description = description;
        this.cssTag = cssTag;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String cssTag() {
        return cssTag;
    }

}
