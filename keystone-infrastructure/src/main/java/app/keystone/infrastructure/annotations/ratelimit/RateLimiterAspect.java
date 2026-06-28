package app.keystone.infrastructure.annotations.ratelimit;

import app.keystone.infrastructure.annotations.ratelimit.implementation.MapRateLimitChecker;
import app.keystone.infrastructure.annotations.ratelimit.implementation.RedisRateLimitChecker;
import java.lang.reflect.Method;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 限流切面处理
 *
 * @author likanug
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedisRateLimitChecker redisRateLimitChecker;

    private final MapRateLimitChecker mapRateLimitChecker;


    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimit rateLimiter) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        log.debug("当前限流方法:{}", method.toGenericString());

        if (Objects.requireNonNull(rateLimiter.cacheType()).isLocal()) {
            mapRateLimitChecker.check(rateLimiter);
        } else {
            redisRateLimitChecker.check(rateLimiter);
        }

    }

}
