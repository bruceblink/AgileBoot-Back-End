package app.keystone.common.enums.common;

import app.keystone.common.enums.DictionaryEnum;
import app.keystone.common.enums.dictionary.CssTag;

/**
 * Scheduled job trigger type.
 *
 * @author likanug
 */
public enum JobTriggerTypeEnum implements DictionaryEnum<Integer> {

    AUTO(1, "自动调度", CssTag.PRIMARY),
    MANUAL(2, "手动执行", CssTag.SUCCESS);

    private final int value;
    private final String description;
    private final String cssTag;

    JobTriggerTypeEnum(int value, String description, String cssTag) {
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
