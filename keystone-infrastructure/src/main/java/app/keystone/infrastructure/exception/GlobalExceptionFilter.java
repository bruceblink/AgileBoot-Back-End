package app.keystone.infrastructure.exception;


import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Internal;
import app.keystone.common.utils.jackson.JacksonUtil;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;


/**
 * 异常过滤器，因为配置的全局异常捕获器只能捕获MVC框架的异常
 * 不能捕获filter的异常
 * @author likanug
 */
@Slf4j
@WebFilter(filterName = "ExceptionFilter", urlPatterns = "/*")
public class GlobalExceptionFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        try {
            chain.doFilter(request, response);
        } catch (ApiException ex) {
            log.error("global filter exceptions", ex);
            String resultJson = JacksonUtil.to(ResponseDTO.fail(ex));
            writeToResponse(response, resultJson);
        } catch (Exception e) {
            log.error("global filter exceptions, unknown error:", e);
            ResponseDTO<Object> responseDTO = ResponseDTO.fail(new ApiException(Internal.INTERNAL_ERROR));
            String resultJson = JacksonUtil.to(responseDTO);
            writeToResponse(response, resultJson);
        }
    }

    private void writeToResponse(ServletResponse response, String resultJson) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(resultJson);
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }
}
