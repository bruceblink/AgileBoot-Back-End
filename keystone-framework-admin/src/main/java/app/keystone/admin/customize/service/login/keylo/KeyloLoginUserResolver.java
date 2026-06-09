package app.keystone.admin.customize.service.login.keylo;

import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.admin.customize.service.login.UserDetailsServiceImpl;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.enums.common.UserStatusEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.i18n.MessageUtils;
import app.keystone.domain.system.user.db.SysUserEntity;
import app.keystone.domain.system.user.db.SysUserService;
import app.keystone.infrastructure.thread.ThreadPoolManager;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class KeyloLoginUserResolver {

    private final SysUserService userService;

    private final UserDetailsServiceImpl userDetailsService;

    private final AsyncTaskFactory asyncTaskFactory;

    public SystemLoginUser resolve(KeyloTokenIdentity keyloIdentity) {
        if (keyloIdentity == null) {
            throw new ApiException(ErrorCode.Business.LOGIN_KEYLO_SUBJECT_MISSING);
        }
        return buildLoginUserByExternalIdentity(keyloIdentity.getKeyloUserId(), keyloIdentity.getKeyloSubject());
    }

    private SystemLoginUser buildLoginUserByExternalIdentity(String keyloUserId, String keyloSubject) {
        SysUserEntity userEntity = null;
        if (StringUtils.hasText(keyloUserId)) {
            userEntity = userService.getUserByExternalUserId(keyloUserId);
        }
        if (userEntity == null && StringUtils.hasText(keyloSubject)) {
            userEntity = userService.getUserByExternalSubject(keyloSubject);
        }
        if (userEntity == null) {
            String identifier = StringUtils.hasText(keyloUserId) ? keyloUserId : keyloSubject;
            ThreadPoolManager.execute(asyncTaskFactory.loginInfoTask(identifier, LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.USER_NON_EXIST", identifier)));
            throw new ApiException(ErrorCode.Business.USER_NON_EXIST, identifier);
        }
        if (!Objects.equals(UserStatusEnum.NORMAL.getValue(), userEntity.getStatus())) {
            ThreadPoolManager.execute(asyncTaskFactory.loginInfoTask(userEntity.getUsername(), LoginStatusEnum.LOGIN_FAIL,
                MessageUtils.message("Business.USER_IS_DISABLE", userEntity.getUsername())));
            throw new ApiException(ErrorCode.Business.USER_IS_DISABLE, userEntity.getUsername());
        }

        return userDetailsService.buildLoginUser(userEntity);
    }
}
