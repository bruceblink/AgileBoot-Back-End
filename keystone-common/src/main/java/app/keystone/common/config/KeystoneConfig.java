package app.keystone.common.config;

import app.keystone.common.constant.Constants;
import app.keystone.common.utils.i18n.MessageUtils;
import java.io.File;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * 读取项目相关配置
 * @author likanug
 */
@Component
@ConfigurationProperties(prefix = "keystone")
public class KeystoneConfig {

    private static KeystoneConfig instance;

    @Autowired
    private MessageSource messageSource;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 版本
     */
    private String version;

    /**
     * 版权年份
     */
    private String copyrightYear;

    /**
     * 实例演示开关
     */
    private boolean demoEnabled;

    /**
     * 上传路径
     */
    private String fileBaseDir;

    /**
     * 获取地址开关
     */
    private boolean addressEnabled;

    /**
     * 验证码类型
     */
    private String captchaType;

    /**
     * rsa private key  静态属性的注入！！ set方法一定不能是static 方法
     */
    private String rsaPrivateKey;

    @PostConstruct
    public void init() {
        instance = this;
        MessageUtils.setMessageSource(messageSource);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public void setFileBaseDir(String fileBaseDir) {
        this.fileBaseDir = fileBaseDir;
    }

    public void setAddressEnabled(boolean addressEnabled) {
        this.addressEnabled = addressEnabled;
    }

    public void setCaptchaType(String captchaType) {
        this.captchaType = captchaType;
    }

    public void setRsaPrivateKey(String rsaPrivateKey) {
        this.rsaPrivateKey = rsaPrivateKey;
    }

    public void setDemoEnabled(boolean demoEnabled) {
        this.demoEnabled = demoEnabled;
    }

    public static String getFileBaseDir() {
        return instance.fileBaseDir + File.separator + Constants.RESOURCE_PREFIX;
    }

    public static boolean isAddressEnabled() {
        return instance.addressEnabled;
    }

    public static String getCaptchaType() {
        return instance.captchaType;
    }

    public static String getRsaPrivateKey() {
        return instance.rsaPrivateKey;
    }

    public static boolean isDemoEnabled() {
        return instance.demoEnabled;
    }

}
