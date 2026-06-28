package app.keystone.infrastructure.annotations;

import app.keystone.infrastructure.annotations.ratelimit.RateLimit;
import app.keystone.infrastructure.annotations.ratelimit.RateLimit.LimitType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitTypeTest {

    @Test
    @SuppressWarnings("deprecation")
    void testCombinedKey() {
        RateLimit mockLimit = mock(RateLimit.class);
        when(mockLimit.key()).thenReturn("Test");
        when(mockLimit.limitType()).thenReturn(LimitType.GLOBAL);

        String combinedKey = LimitType.GLOBAL.generateCombinedKey(mockLimit);

        Assertions.assertEquals("Test:GLOBAL:GLOBAL", combinedKey);
    }

}
