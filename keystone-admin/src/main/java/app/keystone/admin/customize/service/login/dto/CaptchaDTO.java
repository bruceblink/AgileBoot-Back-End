package app.keystone.admin.customize.service.login.dto;

import lombok.Data;

/**
 * @author likanug
 */
@Data
public class CaptchaDTO {

    private Boolean isCaptchaOn;
    private String captchaCodeKey;
    private String captchaCodeImg;

}
