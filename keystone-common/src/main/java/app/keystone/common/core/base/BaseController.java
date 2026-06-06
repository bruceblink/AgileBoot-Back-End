package app.keystone.common.core.base;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * @author likanug
 */
@Slf4j
public class BaseController {

    /**
     *
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parseDate(text));
            }
        });
    }

    private Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd"}) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
                dateFormat.setLenient(false);
                return dateFormat.parse(text.trim());
            } catch (ParseException ignored) {
                continue;
            }
        }
        return null;
    }

    /**
     * 页面跳转
     */
    public String redirect(String url) {
        return "redirect:" + url;
    }


}
