package com.example.aioutfitapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * FreeSWITCH安全配置类
 * 
 * 专门为FreeSWITCH XML请求配置安全规则，优先级高于一般安全规则
 */
@Configuration
@EnableWebSecurity
@Order(1) // 高优先级，确保在WebSecurityConfig之前应用
public class FreeSwitchSecurityConfig {

    /**
     * FreeSWITCH XML请求安全过滤器链
     * 
     * @param http HTTP安全
     * @return 安全过滤器链
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain freeswitchFilterChain(HttpSecurity http) throws Exception {
        // 直接使用字符串路径而非AntPathRequestMatcher对象
        http
            .securityMatcher("/freeswitch/xml", "/api/freeswitch/xml", 
                            "/freeswitch/test", "/api/freeswitch/test")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        
        return http.build();
    }
} 