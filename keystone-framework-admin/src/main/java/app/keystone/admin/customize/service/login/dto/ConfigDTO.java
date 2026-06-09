package app.keystone.admin.customize.service.login.dto;

import app.keystone.common.enums.dictionary.DictionaryData;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * @author likanug
 */
@Data
public class ConfigDTO {

    private Boolean isCaptchaOn;

    private Map<String, List<DictionaryData>> dictionary;

}
