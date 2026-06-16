package app.keystone.infrastructure.exception;

import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode;
import app.keystone.common.exception.error.ErrorCode.Business;
import app.keystone.common.exception.error.ErrorCode.Client;
import app.keystone.common.exception.error.ErrorCode.Internal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * @author likanug
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionInterceptor {

    private static final String INVALID_PARAMETER_MESSAGE = "参数错误";

    /**
     * 权限校验异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseDTO<?> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.error("请求地址'{}',权限校验失败'{}'", request.getRequestURI(), e.getMessage());
        return ResponseDTO.fail(new ApiException(Business.PERMISSION_NOT_ALLOWED_TO_OPERATE));
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseDTO<?> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
        HttpServletRequest request) {
        log.error("请求地址'{}',不支持'{}'请求", request.getRequestURI(), e.getMethod());
        return ResponseDTO.fail(new ApiException(Client.COMMON_REQUEST_METHOD_INVALID, e.getMethod()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseDTO<?>> handleNoResourceFoundException(NoResourceFoundException e,
        HttpServletRequest request) {
        log.warn("请求地址'{}',资源不存在'{}'", request.getRequestURI(), e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ResponseDTO.build(null, HttpStatus.NOT_FOUND.value(), "请求资源不存在"));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ApiException.class)
    public ResponseDTO<?> handleServiceException(ApiException e) {
        log.error(e.getMessage(), e);
        return ResponseDTO.fail(e, e.getPayload());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseDTO<?> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String errorMsg = String.format("请求地址'%s',发生未知异常.", request.getRequestURI());
        log.error(errorMsg, e);
        // 不将原始异常信息返回给客户端，避免泄露内部实现细节
        return ResponseDTO.fail(new ApiException(Internal.INTERNAL_ERROR));
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseDTO<?> handleException(Exception e, HttpServletRequest request) {
        String errorMsg = String.format("请求地址'%s',发生未知异常.", request.getRequestURI());
        log.error(errorMsg, e);
        return ResponseDTO.fail(new ApiException(Internal.INTERNAL_ERROR));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseDTO<?> handleBindException(BindException e) {
        log.error(e.getMessage(), e);
        return invalidParameterResponse(resolveValidationMessage(e.getBindingResult()));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseDTO<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        return invalidParameterResponse(resolveValidationMessage(e.getBindingResult()));
    }

    /**
     * 请求参数异常
     */
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpMessageNotReadableException.class,
    })
    public ResponseDTO<?> handleInvalidParameterException(Exception e, HttpServletRequest request) {
        log.error("请求地址'{}',参数错误'{}'", request.getRequestURI(), e.getMessage(), e);
        return invalidParameterResponse();
    }

    /**
     * Bean Validation异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseDTO<?> handleConstraintViolationException(ConstraintViolationException e,
        HttpServletRequest request) {
        log.error("请求地址'{}',参数错误'{}'", request.getRequestURI(), e.getMessage(), e);
        return invalidParameterResponse(resolveValidationMessage(e));
    }

    private ResponseDTO<?> invalidParameterResponse() {
        return invalidParameterResponse(INVALID_PARAMETER_MESSAGE);
    }

    private ResponseDTO<?> invalidParameterResponse(String message) {
        return ResponseDTO.build(null, ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID.code(),
            hasText(message) ? message : INVALID_PARAMETER_MESSAGE);
    }

    private String resolveValidationMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
            .filter(this::isValidationError)
            .map(ObjectError::getDefaultMessage)
            .filter(GlobalExceptionInterceptor::hasText)
            .findFirst()
            .orElse(INVALID_PARAMETER_MESSAGE);
    }

    private String resolveValidationMessage(ConstraintViolationException exception) {
        return exception.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .filter(GlobalExceptionInterceptor::hasText)
            .findFirst()
            .orElse(INVALID_PARAMETER_MESSAGE);
    }

    private boolean isValidationError(ObjectError error) {
        return !(error instanceof FieldError fieldError) || !fieldError.isBindingFailure();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }


}
