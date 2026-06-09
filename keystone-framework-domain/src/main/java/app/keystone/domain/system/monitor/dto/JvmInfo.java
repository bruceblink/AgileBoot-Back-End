package app.keystone.domain.system.monitor.dto;

import app.keystone.common.constant.Constants;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.Data;

/**
 * JVM相关信息
 *
 * @author ruoyi
 */
@Data
public class JvmInfo {

    private static final DateTimeFormatter NORM_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    /**
     * 当前JVM占用的内存总数(M)
     */
    private double total;

    /**
     * JVM最大可用内存总数(M)
     */
    private double max;

    /**
     * JVM空闲内存(M)
     */
    private double free;

    /**
     * JDK版本
     */
    private String version;

    /**
     * JDK路径
     */
    private String home;

    public double getTotal() {
        return divide(total, Constants.MB);
    }

    public double getMax() {
        return divide(max, Constants.MB);
    }

    public double getFree() {
        return divide(free, Constants.MB);
    }

    public double getUsed() {
        return divide(total - free, Constants.MB);
    }

    public double getUsage() {
        return divide((total - free) * 100, total);
    }

    /**
     * 获取JDK名称
     */
    public String getName() {
        return ManagementFactory.getRuntimeMXBean().getVmName();
    }

    /**
     * JDK启动时间
     */
    public String getStartTime() {
        return NORM_DATETIME_FORMATTER.format(Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()));
    }

    /**
     * JDK运行时间
     */
    public String getRunTime() {
        long startMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
        Duration duration = Duration.between(Instant.ofEpochMilli(startMillis), Instant.now());
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }

    /**
     * 运行参数
     */
    public String getInputArgs() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
    }

    private double divide(double dividend, double divisor) {
        if (divisor == 0D) {
            return 0D;
        }
        return BigDecimal.valueOf(dividend)
            .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
