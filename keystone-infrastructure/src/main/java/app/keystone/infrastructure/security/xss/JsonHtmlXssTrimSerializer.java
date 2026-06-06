package app.keystone.infrastructure.security.xss;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * 直接将html标签去掉
 * @author likanug
 */
public class JsonHtmlXssTrimSerializer extends JsonDeserializer<String> {

    public JsonHtmlXssTrimSerializer() {
        super();
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext context) throws IOException {
        String value = p.getValueAsString();
        if( value != null) {
            return value.replaceAll("<[^>]*>", "");
        }
        return null;
    }

    @Override
    public Class<String> handledType() {
        return String.class;
    }

}
