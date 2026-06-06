package app.keystone.infrastructure.i18n;

import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCodeInterface;
import app.keystone.common.utils.i18n.MessageUtils;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 检测 未添加到i18n文件(messages.properties)中的message
 * @author likanug
 */
@Component
@Slf4j
public class MessageI18nCheckerRunner implements ApplicationRunner {

    @Value("keystone.checkI18nKey")
    private String checkI18nKey;

    private static final Object[] ALL_ERROR_CODES = Stream.of(
            ErrorCode.Internal.values(),
            ErrorCode.External.values(),
            ErrorCode.Client.values(),
            ErrorCode.Business.values())
        .flatMap(Stream::of)
        .toArray();

    @Override
    public void run(ApplicationArguments args) {
        if (Boolean.parseBoolean(checkI18nKey)) {
            checkEveryMessage();
        }
    }

    /**
     * 如果想支持i18n, 请把对应的错误码描述填到 /resources/i18n/messages.properties 文件中
     */
    public void checkEveryMessage() {
        for (Object errorCode : ALL_ERROR_CODES) {
            ErrorCodeInterface errorInterface = (ErrorCodeInterface) errorCode;
            try {
                MessageUtils.message(errorInterface.i18nKey());
            } catch (Exception e) {
                log.warn("could not find i18n message for:{}  in the file /resources/i18n/messages.properties.",
                    errorInterface.i18nKey());
            }
        }
    }
}
