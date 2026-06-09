package app.keystone.admin.customize.config;

import app.keystone.admin.customize.async.AsyncTaskFactory;
import app.keystone.admin.customize.service.login.LoginService;
import app.keystone.admin.customize.service.login.TokenService;
import app.keystone.admin.customize.service.login.UserDetailsServiceImpl;
import app.keystone.common.core.dto.ResponseDTO;
import app.keystone.common.enums.common.LoginStatusEnum;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Client;
import app.keystone.common.utils.ServletHolderUtil;
import app.keystone.common.utils.jackson.JacksonUtil;
import app.keystone.infrastructure.thread.ThreadPoolManager;
import app.keystone.infrastructure.user.web.SystemLoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.filter.CorsFilter;

/**
 * 主要配置登录流程逻辑涉及以下几个类
 * @see this#unauthorizedHandler()  用于用户未授权或登录失败处理
 * @see this#logOutSuccessHandler 用于退出登录成功后的逻辑
 * @see JwtAuthenticationTokenFilter#doFilter token的校验和刷新
 * @see LoginService#login 登录逻辑
 * @author likanug
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenService tokenService;

    private final AsyncTaskFactory asyncTaskFactory;

    /**
     * token认证过滤器
     */
    private final JwtAuthenticationTokenFilter jwtTokenFilter;

    private final UserDetailsService userDetailsService;

    /**
     * 跨域过滤器
     */
    private final CorsFilter corsFilter;

    private final Environment environment;


    /**
     * 登录异常处理类
     * 用户未登陆的话  在这个Bean中处理
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedHandler() {
        return (request, response, exception) -> {
            ResponseDTO<Object> responseDTO = ResponseDTO.fail(
                new ApiException(Client.COMMON_NO_AUTHORIZATION, request.getRequestURI())
            );
            ServletHolderUtil.renderString(response, JacksonUtil.to(responseDTO));
        };
    }


    /**
     *  退出成功处理类 返回成功
     *  在SecurityConfig类当中 定义了/logout 路径对应处理逻辑
     */
    @Bean
    public LogoutSuccessHandler logOutSuccessHandler() {
        return (request, response, authentication) -> {
            String token = tokenService.getTokenFromRequest(request);
            SystemLoginUser loginUser = tokenService.removeLoginUserByToken(token);
            if (loginUser != null) {
                String userName = loginUser.getUsername();
                // 记录用户退出日志
                ThreadPoolManager.execute(asyncTaskFactory.loginInfoTask(
                    userName, LoginStatusEnum.LOGOUT, LoginStatusEnum.LOGOUT.description()));
            }
            ServletHolderUtil.renderString(response, JacksonUtil.to(ResponseDTO.ok()));
        };
    }

    /**
     * 强散列哈希加密实现
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * 鉴权管理类
     * @see UserDetailsServiceImpl#loadUserByUsername
     */
    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
        return authManagerBuilder.build();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
            // CSRF禁用，因为不使用session
            .csrf(csrf -> csrf.disable())
            // 认证失败处理类
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler()))
            // 基于token，所以不需要session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 过滤请求
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(
                        "/login", "/login/keylo", "/refresh-token", "/logout-refresh-token", "/register", "/getConfig",
                        "/health", "/captchaImage"
                    ).anonymous()
                    .requestMatchers("/login/rsa-public-key").permitAll()
                    .requestMatchers(
                        "/BehaviorConfig/**",
                        "/Berthing/**",
                        "/Camera/**",
                        "/Interact/**",
                        "/Situation/**"
                    ).permitAll()
                    .requestMatchers("/druid/**").authenticated();
                if (isSwaggerEnabled()) {
                    auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll();
                } else {
                    auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**").authenticated()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**").authenticated();
                }
                auth.requestMatchers(HttpMethod.GET, "/", "/*.html", "/*.css", "/*.js", "/profile/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/**/*.html", "/**/*.css", "/**/*.js").permitAll();
                auth.anyRequest().authenticated();
            })
            // 允许同源的 frame 嵌套（用于 Druid 控制台等），防止外部站点点击劫持
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        httpSecurity.logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logOutSuccessHandler()));
        // 添加JWT filter   需要一开始就通过token识别出登录用户 并放到上下文中   所以jwtFilter需要放前面
        httpSecurity.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        // 添加CORS filter
        httpSecurity.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);

        return httpSecurity.build();
    }

    boolean isSwaggerEnabled() {
        Boolean apiDocsEnabled = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, false);
        Boolean swaggerUiEnabled = environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, false);
        return apiDocsEnabled && swaggerUiEnabled;
    }

}
