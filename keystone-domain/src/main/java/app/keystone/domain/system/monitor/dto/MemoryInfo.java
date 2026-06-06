package app.keystone.domain.system.monitor.dto;

import app.keystone.common.constant.Constants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;

/**
 * 內存相关信息
 *
 * @author likanug
 */
@Data
public class MemoryInfo {

    /**
     * 内存总量
     */
    private double total;

    /**
     * 已用内存
     */
    private double used;

    /**
     * 剩余内存
     */
    private double free;

    public double getTotal() {
        return divide(total, Constants.GB);
    }

    public double getUsed() {
        return divide(used, Constants.GB);
    }

    public double getFree() {
        return divide(free, Constants.GB);
    }

    public double getUsage() {
        return divide(used * 100, total);
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
