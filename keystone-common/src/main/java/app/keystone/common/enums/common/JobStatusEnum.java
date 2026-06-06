package app.keystone.common.enums.common;

import app.keystone.common.enums.DictionaryEnum;
import app.keystone.common.enums.dictionary.CssTag;

/**
 * Scheduled job status.
 * @author likanug
 */
public enum JobStatusEnum implements DictionaryEnum<Integer> {

    /**
     * Scheduled job is enabled.
     */
    ENABLE(1, "正常", CssTag.PRIMARY),

    /**
     * Scheduled job is paused.
     */
    PAUSE(0, "暂停", CssTag.DANGER);

    private final int value;
    private final String description;
    private final String cssTag;

    JobStatusEnum(int value, String description, String cssTag) {
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
