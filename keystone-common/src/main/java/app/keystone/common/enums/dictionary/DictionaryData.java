package app.keystone.common.enums.dictionary;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典模型类
 * @author likanug
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictionaryData {

    private String label;
    private Integer value;
    private String cssTag;

}
