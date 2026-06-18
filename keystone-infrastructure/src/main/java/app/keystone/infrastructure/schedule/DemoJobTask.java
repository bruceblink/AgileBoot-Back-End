package app.keystone.infrastructure.schedule;

import app.keystone.common.annotation.JobTask;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Harmless scheduled job examples for validating database-backed job configuration.
 *
 * @author likanug
 */
@Slf4j
@Component("demoJobTask")
public class DemoJobTask {

    @JobTask(name = "打印心跳日志", group = "示例任务", description = "用于验证定时任务是否能按计划触发")
    public void printHeartbeat() {
        log.info("demo scheduled job heartbeat, currentTime={}", LocalDateTime.now());
    }

    @JobTask(name = "模拟缓存刷新", group = "示例任务", description = "用于验证业务型无参任务调用")
    public void refreshDemoCache() {
        log.info("demo scheduled job refreshed cache placeholder, currentTime={}", LocalDateTime.now());
    }

    @JobTask(name = "模拟耗时任务", group = "示例任务", description = "用于验证禁止并发执行配置")
    public void simulateLongRunningJob() {
        try {
            log.info("demo long running scheduled job started");
            TimeUnit.SECONDS.sleep(3);
            log.info("demo long running scheduled job finished");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("demo long running scheduled job interrupted", e);
        }
    }
}
