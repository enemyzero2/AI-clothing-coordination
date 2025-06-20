package com.example.aioutfitapp.security;

import com.example.aioutfitapp.security.jwt.AuthEntryPointJwt;
import com.example.aioutfitapp.security.jwt.AuthTokenFilter;
import com.example.aioutfitapp.security.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Web安全配置类
 * 
 * 配置Spring Security和JWT认证
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Order(2) // 优先级低于FreeSwitchSecurityConfig
public class WebSecurityConfig {
    
    /**
     * 用户详情服务
     */
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    /**
     * JWT认证入口点
     */
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    /**
     * JWT认证过滤器
     * 
     * @return JWT认证过滤器
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    /**
     * DAO认证提供者
     * 
     * @return DAO认证提供者
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        
        return authProvider;
    }

    /**
     * 认证管理器
     * 
     * @param authConfig 认证配置
     * @return 认证管理器
     * @throws Exception 异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 密码编码器
     * 
     * @return 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链
     * 
     * @param http HTTP安全
     * @return 安全过滤器链
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护以允许POST请求
            .csrf(csrf -> csrf.disable())
            
            // 异常处理
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            
            // 会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 请求授权配置
            .authorizeHttpRequests(auth -> {
                try {
                    // FreeSWITCH XML请求 - 完全放行，不需要认证
                    auth
                        .requestMatchers("/api/freeswitch/xml").permitAll()
                        .requestMatchers("/freeswitch/xml").permitAll()
                        
                        // 身份验证端点
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        
                        // 测试端点
                        .requestMatchers("/api/test/**").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        
                        // 所有其他请求需要认证
                        .anyRequest().authenticated();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
            
            // CORS配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
        // 设置认证提供者
        http.authenticationProvider(authenticationProvider());
        
        // 添加JWT过滤器
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * CORS配置源
     * 
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 