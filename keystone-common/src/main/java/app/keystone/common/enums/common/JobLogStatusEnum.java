package app.keystone.common.enums.common;

import app.keystone.common.enums.DictionaryEnum;
import app.keystone.common.enums.dictionary.CssTag;

/**
 * Scheduled job execution status.
 *
 * @author likanug
 */
public enum JobLogStatusEnum implements DictionaryEnum<Integer> {

    FAIL(0, "失败", CssTag.DANGER),
    SUCCESS(1, "成功", CssTag.SUCCESS),
    SKIPPED(2, "跳过", CssTag.WARNING);

    private final int value;
    private final String description;
    private final String cssTag;

    JobLogStatusEnum(int value, String description, String cssTag) {
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
