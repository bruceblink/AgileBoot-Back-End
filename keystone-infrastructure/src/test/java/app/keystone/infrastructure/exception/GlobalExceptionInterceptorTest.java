package app.keystone.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionInterceptorTest {

    private final GlobalExceptionInterceptor interceptor = new GlobalExceptionInterceptor();

    @Test
    void handleBindExceptionShouldHideBindingDetails() {
        BindException exception = new BindException(new Object(), "query");
        exception.addError(new FieldError("query", "userId", "Failed to convert property value"));

        ResponseDTO<?> response = interceptor.handleBindException(exception);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("参数错误");
    }

    @Test
    void handleTypeMismatchShouldReturnParameterError() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException("%", Long.class,
            "userId", null, new NumberFormatException("%"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/users/%");

        ResponseDTO<?> response = interceptor.handleInvalidParameterException(exception, request);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("参数错误");
    }
}
