package app.keystone.common.utils.jackson;

/**
 * @author likanug
 */
public class JacksonException extends RuntimeException {

    public JacksonException(String message, Exception e) {
        super(message, e);
    }

}
