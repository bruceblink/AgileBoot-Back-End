package app.keystone.common.enums.dictionary;

import app.keystone.common.enums.DictionaryEnum;
import lombok.Data;

/**
 * 字典模型类
 * @author likanug
 */
@Data
public class DictionaryData {

    private String label;
    private Integer value;
    private String cssTag;

    @SuppressWarnings("rawtypes")
    public DictionaryData(DictionaryEnum enumType) {
        if (enumType != null) {
            this.label = enumType.description();
            this.value = (Integer) enumType.getValue();
            this.cssTag = enumType.cssTag();
        }
    }

}
