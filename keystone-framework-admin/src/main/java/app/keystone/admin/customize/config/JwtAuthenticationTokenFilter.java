package app.keystone.admin.customize.config;

import app.keystone.admin.customize.service.login.TokenService;
import app.keystone.admin.customize.service.login.keylo.KeyloProperties;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenIdentity;
import app.keystone.admin.customize.service.login.keylo.KeyloTokenVerifier;
import app.keystone.common.exception.ApiException;
import app.keystone.infrastructure.user.AuthenticationUtils;
import app.keystone.infrastructure.user.web.DataScopeEnum;
import app.keystone.infrastructure.user.web.RoleInfo;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * token过滤器 验证token有效性
 * 继承OncePerRequestFilter类的话  可以确保只执行filter一次， 避免执行多次
 * @author likanug
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final KeyloProperties keyloProperties;

    private final KeyloTokenVerifier keyloTokenVerifier;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/refresh-token".equals(request.getServletPath())
            || "/logout-refresh-token".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        SystemLoginUser loginUser = getLoginUser(request);
        if (loginUser != null && AuthenticationUtils.getAuthentication() == null) {
            // 如果没有将当前登录用户放入到上下文中的话，会认定用户未授权，返回用户未登陆的错误
            putCurrentLoginUserIntoContext(request, loginUser);

            log.debug("request process in jwt token filter. get login user id: {}", loginUser.getUserId());
        }
        chain.doFilter(request, response);
    }

    private SystemLoginUser getLoginUser(HttpServletRequest request) {
        String rawToken = tokenService.getTokenFromRequest(request);
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        try {
            return tokenService.getLoginUserByTokenSilently(rawToken);
        } catch (ApiException keystoneTokenException) {
            if (!keyloProperties.isEnabled()) {
                throw keystoneTokenException;
            }
            try {
                KeyloTokenIdentity identity = keyloTokenVerifier.verify(rawToken);
                return buildTrustedKeyloLoginUser(identity);
            } catch (ApiException keyloTokenException) {
                throw keyloTokenException;
            }
        }
    }

    private SystemLoginUser buildTrustedKeyloLoginUser(KeyloTokenIdentity keyloIdentity) {
        String username = keyloIdentity.getKeyloSubject();
        long userId = -Math.abs((long) username.hashCode());
        RoleInfo roleInfo = new RoleInfo(RoleInfo.ADMIN_ROLE_ID, RoleInfo.ADMIN_ROLE_KEY, DataScopeEnum.ALL,
            Set.of(), RoleInfo.ADMIN_PERMISSIONS, Set.of());
        SystemLoginUser loginUser = new SystemLoginUser(userId, true, username, null, roleInfo, 0L);
        loginUser.setAuthorities(new ArrayList<>(List.of(new SimpleGrantedAuthority(RoleInfo.ALL_PERMISSIONS))));
        return loginUser;
    }


    private void putCurrentLoginUserIntoContext(HttpServletRequest request, SystemLoginUser loginUser) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(loginUser,
            null, loginUser.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

}
