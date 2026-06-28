package app.keystone.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.error.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionInterceptorTest {

    private final GlobalExceptionInterceptor interceptor = new GlobalExceptionInterceptor();

    @Test
    void handleBindExceptionShouldHideBindingDetails() {
        BindException exception = new BindException(new Object(), "query");
        exception.addError(new FieldError("query", "userId", "%", true, null, null,
            "Failed to convert property value"));

        ResponseDTO<?> response = interceptor.handleBindException(exception);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("参数错误");
    }

    @Test
    void handleBindExceptionShouldReturnValidationMessage() {
        BindException exception = new BindException(new Object(), "query");
        exception.addError(new FieldError("query", "status", 2, false, null, null, "用户状态值无效"));

        ResponseDTO<?> response = interceptor.handleBindException(exception);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("用户状态值无效");
    }

    @Test
    void handleMethodArgumentNotValidShouldReturnValidationMessage() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("add", Object.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "addUserCommand");
        bindingResult.addError(new FieldError("addUserCommand", "nickname", "", false, null, null,
            "用户昵称不能为空"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseDTO<?> response = interceptor.handleMethodArgumentNotValidException(exception);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("用户昵称不能为空");
    }

    @Test
    void handleConstraintViolationShouldReturnValidationMessage() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<ValidationTarget>> violations = validator.validate(new ValidationTarget());
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/users");

        ResponseDTO<?> response = interceptor.handleConstraintViolationException(exception, request);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("名称不能为空");
    }

    @Test
    void handleTypeMismatchShouldReturnParameterError() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("getUser", Long.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException("%", Long.class,
            "userId", parameter, new NumberFormatException("%"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/system/users/%");

        ResponseDTO<?> response = interceptor.handleInvalidParameterException(exception, request);

        assertThat(response.getCode()).isEqualTo(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code());
        assertThat(response.getMsg()).isEqualTo("参数错误");
    }

    private static class TestController {

        @SuppressWarnings("unused")
        void add(Object command) {
        }

        @SuppressWarnings("unused")
        void getUser(Long userId) {
        }
    }

    private static class ValidationTarget {

        private String name = "";

        @NotBlank(message = "名称不能为空")
        public String getName() {
            return name;
        }
    }
}
