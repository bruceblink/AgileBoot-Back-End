package app.keystone.infrastructure.annotations.ratelimit;

import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.utils.ip.IpUtil;
import app.keystone.infrastructure.user.app.AppLoginUser;
import app.keystone.infrastructure.user.base.BaseLoginUser;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Builds stable and collision-resistant cache keys for {@link RateLimit}.
 */
public final class RateLimitKeyGenerator {

    private static final String GLOBAL_SCOPE = "GLOBAL";

    private RateLimitKeyGenerator() {
    }

    public static String generate(RateLimit rateLimit) {
        Objects.requireNonNull(rateLimit, "rateLimit must not be null");
        RateLimit.LimitType limitType = Objects.requireNonNull(rateLimit.limitType(), "limitType must not be null");

        return switch (limitType) {
            case GLOBAL -> buildKey(rateLimit, GLOBAL_SCOPE);
            case IP -> buildKey(rateLimit, clientIp());
            case SYSTEM_USER -> buildKey(rateLimit, loginUserKey(SystemLoginUser.class));
            case APP_USER -> buildKey(rateLimit, loginUserKey(AppLoginUser.class));
        };
    }

    private static String buildKey(RateLimit rateLimit, String discriminator) {
        return join(normalizedBaseKey(rateLimit.key()), rateLimit.limitType().name(), discriminator);
    }

    private static String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID,
                "IP rate limit requires a servlet request");
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String clientIp = IpUtil.getClientIp(request);
        if (clientIp == null || clientIp.isBlank()) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "client IP is empty");
        }
        return clientIp;
    }

    private static <T extends BaseLoginUser> String loginUserKey(Class<T> userType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (authentication == null || !authentication.isAuthenticated() || !userType.isInstance(principal)) {
            throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION, "rate limit");
        }
        BaseLoginUser loginUser = userType.cast(principal);

        if (loginUser.getUserId() != null) {
            return join("USER", String.valueOf(loginUser.getUserId()));
        }
        if (loginUser.getUsername() != null && !loginUser.getUsername().isBlank()) {
            return join("USERNAME", loginUser.getUsername());
        }
        if (loginUser.getCachedKey() != null && !loginUser.getCachedKey().isBlank()) {
            return join("CACHE", loginUser.getCachedKey());
        }

        throw new ApiException(ErrorCode.Client.COMMON_NO_AUTHORIZATION, "rate limit");
    }

    private static String normalizedBaseKey(String key) {
        String baseKey = key == null || key.isBlank() ? RateLimitKey.PREFIX : key.trim();
        while (baseKey.endsWith(":")) {
            baseKey = baseKey.substring(0, baseKey.length() - 1);
        }
        return baseKey;
    }

    private static String join(String firstSegment, String... otherSegments) {
        StringBuilder builder = new StringBuilder(escapeSegment(firstSegment));
        for (String segment : otherSegments) {
            builder.append(':').append(escapeSegment(segment));
        }
        return builder.toString();
    }

    private static String escapeSegment(String segment) {
        return segment.replace("\\", "\\\\").replace(":", "\\:");
    }
}
