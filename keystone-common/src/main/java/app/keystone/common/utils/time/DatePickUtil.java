package app.keystone.common.utils.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * @author likanug
 */
@Slf4j
public class DatePickUtil {

    private DatePickUtil() {
    }

    /**
     * 安全地获取日期的一天开始时间, date为null 则返回null DateUtil.beginOfDay(date) 如果传null 会NPE
     *
     * @param date 当前日期
     * @return 日期的一天开始时间
     */
    public static Date getBeginOfTheDay(Date date) {
        if (date == null) {
            return null;
        }
        try {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDateTime beginOfDay = LocalDateTime.of(localDate, LocalTime.MIN);
            return Date.from(beginOfDay.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("pick begin of the day failed, due to: ", e);
        }
        return null;
    }

    /**
     * 安全地获取日期的一天结束时间, date为null 则返回null。 避免NPE
     *  DateUtil.endOfDay(date) 如果传null 会NPE
     * @param date 23:59:59
     * @return 日期的一天结束时间
     */
    public static Date getEndOfTheDay(Date date) {
        if (date == null) {
            return null;
        }

        try {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDateTime endOfDay = LocalDateTime.of(localDate, LocalTime.of(23, 59, 59));
            return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("pick end of the day failed, due to: ", e);
        }
        return null;
    }



}
