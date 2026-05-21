package app.keystone.domain.system.log;

import app.keystone.common.core.page.PageDTO;
import app.keystone.common.enums.common.BusinessTypeEnum;
import app.keystone.common.enums.common.OperationStatusEnum;
import app.keystone.common.enums.common.OperatorTypeEnum;
import app.keystone.common.enums.common.RequestMethodEnum;
import app.keystone.common.utils.ip.IpRegionUtil;
import app.keystone.domain.common.command.BulkOperationCommand;
import app.keystone.domain.common.cache.CacheCenter;
import app.keystone.domain.system.dept.db.SysDeptEntity;
import app.keystone.domain.system.log.command.AddOperationLogCommand;
import app.keystone.domain.system.log.dto.LoginLogDTO;
import app.keystone.domain.system.log.query.LoginLogQuery;
import app.keystone.domain.system.log.dto.OperationLogDTO;
import app.keystone.domain.system.log.query.OperationLogQuery;
import app.keystone.domain.system.log.db.SysLoginInfoEntity;
import app.keystone.domain.system.log.db.SysOperationLogEntity;
import app.keystone.domain.system.log.db.SysLoginInfoService;
import app.keystone.domain.system.log.db.SysOperationLogService;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author valarchie
 */
@Service
@RequiredArgsConstructor
public class LogApplicationService {

    private final SysLoginInfoService loginInfoService;

    private final SysOperationLogService operationLogService;

    public PageDTO<LoginLogDTO> getLoginInfoList(LoginLogQuery query) {
        Page<SysLoginInfoEntity> page = loginInfoService.page(query.toPage(), query.toQueryWrapper());
        List<LoginLogDTO> records = page.getRecords().stream().map(LoginLogDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public void deleteLoginInfo(BulkOperationCommand<Long> deleteCommand) {
        QueryWrapper<SysLoginInfoEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("info_id", deleteCommand.getIds());
        loginInfoService.remove(queryWrapper);
    }

    public PageDTO<OperationLogDTO> getOperationLogList(OperationLogQuery query) {
        Page<SysOperationLogEntity> page = operationLogService.page(query.toPage(), query.toQueryWrapper());
        List<OperationLogDTO> records = page.getRecords().stream().map(OperationLogDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public void deleteOperationLog(BulkOperationCommand<Long> deleteCommand) {
        operationLogService.removeBatchByIds(deleteCommand.getIds());
    }

    public void addOperationLog(AddOperationLogCommand command, SystemLoginUser loginUser, String requestIp,
        String currentRequestUrl, String currentRequestMethod) {
        SysOperationLogEntity entity = new SysOperationLogEntity();
        BeanUtils.copyProperties(command, entity);
        fillDefaultOperationLogFields(entity, loginUser, requestIp, currentRequestUrl, currentRequestMethod);
        operationLogService.save(entity);
    }

    private void fillDefaultOperationLogFields(SysOperationLogEntity entity, SystemLoginUser loginUser,
        String requestIp, String currentRequestUrl, String currentRequestMethod) {
        entity.setBusinessType(defaultValue(entity.getBusinessType(), BusinessTypeEnum.OTHER.getValue()));
        entity.setRequestMethod(defaultValue(entity.getRequestMethod(), parseRequestMethod(currentRequestMethod)));
        entity.setRequestUrl(defaultString(entity.getRequestUrl(), currentRequestUrl));
        entity.setCalledMethod(defaultString(entity.getCalledMethod(), ""));
        entity.setRequestModule(defaultString(entity.getRequestModule(), ""));
        entity.setOperatorType(defaultValue(entity.getOperatorType(), OperatorTypeEnum.WEB.getValue()));
        entity.setStatus(defaultValue(entity.getStatus(), OperationStatusEnum.SUCCESS.getValue()));
        entity.setOperationTime(entity.getOperationTime() == null ? new Date() : entity.getOperationTime());
        entity.setOperatorIp(StringUtils.defaultString(requestIp));
        entity.setOperatorLocation(IpRegionUtil.getBriefLocationByIp(entity.getOperatorIp()));

        if (loginUser != null) {
            entity.setUserId(loginUser.getUserId());
            entity.setUsername(loginUser.getUsername());
            entity.setDeptId(loginUser.getDeptId());
        }
        entity.setDeptName(defaultString(entity.getDeptName(), getDeptName(entity.getDeptId())));
        entity.setOperationParam(defaultString(entity.getOperationParam(), ""));
        entity.setOperationResult(defaultString(entity.getOperationResult(), ""));
        entity.setErrorStack(defaultString(entity.getErrorStack(), ""));
    }

    private Integer parseRequestMethod(String requestMethod) {
        try {
            return RequestMethodEnum.valueOf(requestMethod).getValue();
        } catch (Exception e) {
            return RequestMethodEnum.UNKNOWN.getValue();
        }
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) {
            return "";
        }
        SysDeptEntity dept = CacheCenter.deptCache().get(deptId);
        return dept == null ? "" : dept.getDeptName();
    }

    private <T> T defaultValue(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String defaultString(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? StringUtils.defaultString(defaultValue) : value;
    }

}
