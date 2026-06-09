package app.keystone.domain.system.monitor.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;

/**
 * CPU相关信息
 *
 * @author ruoyi
 */
@Data
public class CpuInfo {

    /**
     * 核心数
     */
    private int cpuNum;

    /**
     * CPU总的使用率
     */
    private double total;

    /**
     * CPU系统使用率
     */
    private double sys;

    /**
     * CPU用户使用率
     */
    private double used;

    /**
     * CPU当前等待率
     */
    private double wait;

    /**
     * CPU当前空闲率
     */
    private double free;

    public double getTotal() {
        return BigDecimal.valueOf(total * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double getSys() {
        return divideToPercent(sys, total);
    }

    public double getUsed() {
        return divideToPercent(used, total);
    }

    public double getWait() {
        return divideToPercent(wait, total);
    }

    public double getFree() {
        return divideToPercent(free, total);
    }

    private double divideToPercent(double value, double totalValue) {
        if (totalValue == 0D) {
            return 0D;
        }
        return BigDecimal.valueOf(value * 100)
            .divide(BigDecimal.valueOf(totalValue), 2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}

