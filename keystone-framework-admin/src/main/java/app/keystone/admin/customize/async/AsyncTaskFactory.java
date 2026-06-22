package app.keystone.admin.customize.async;

import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.ip.IpUtil;
import app.keystone.common.utils.ip.IpRegionUtil;
import app.keystone.domain.system.log.db.SysLoginInfoEntity;
import app.keystone.domain.system.log.db.SysLoginInfoService;
import app.keystone.domain.system.log.db.SysOperationLogEntity;
import app.keystone.domain.system.log.db.SysOperationLogService;
import eu.bitwalker.useragentutils.UserAgent;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异步工厂（产生任务用）
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class AsyncTaskFactory {

    private final SysLoginInfoService loginInfoService;
    private final SysOperationLogService operationLogService;

    public AsyncTaskFactory(SysLoginInfoService loginInfoService, SysOperationLogService operationLogService) {
        this.loginInfoService = loginInfoService;
        this.operationLogService = operationLogService;
    }

    /**
     * 记录登录信息
     *
     * @param username        用户名
     * @param loginStatusEnum 状态
     * @param message         消息
     * @return 任务task
     */
    public Runnable loginInfoTask(final String username, final LoginStatusEnum loginStatusEnum, final String message) {
        final UserAgent userAgent = UserAgent.parseUserAgentString(
            ServletHolderUtil.getRequest().getHeader("User-Agent"));
        final String browser = userAgent.getBrowser() != null ? userAgent.getBrowser().getName() : "";
        final String ip = IpUtil.getClientIp(ServletHolderUtil.getRequest());
        final String address = IpRegionUtil.getBriefLocationByIp(ip);
        final String os = userAgent.getOperatingSystem() != null ? userAgent.getOperatingSystem().getName() : "";

        log.info("ip: {}, address: {}, username: {}, loginStatusEnum: {}, message: {}", ip, address, username,
            loginStatusEnum, message);
        return () -> {
            SysLoginInfoEntity loginInfo = new SysLoginInfoEntity();
            loginInfo.setUsername(username);
            loginInfo.setIpAddress(ip);
            loginInfo.setLoginLocation(address);
            loginInfo.setBrowser(browser);
            loginInfo.setOperationSystem(os);
            loginInfo.setMsg(message);
            loginInfo.setLoginTime(new Date());
            loginInfo.setStatus(loginStatusEnum.getValue());
            loginInfoService.save(loginInfo);
        };
    }

    /**
     * 操作日志记录
     *
     * @param operationLog 操作日志信息
     * @return 任务task
     */
    public Runnable recordOperationLog(final SysOperationLogEntity operationLog) {
        return () -> {
            operationLog.setOperatorLocation(IpRegionUtil.getBriefLocationByIp(operationLog.getOperatorIp()));
            operationLogService.save(operationLog);
        };
    }

}
