package com.example.aioutfitapp.security.jwt;

import com.example.aioutfitapp.security.service.UserDetailsServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器
 * 
 * 用于从请求中提取和验证JWT令牌
 */
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {
    
    /**
     * JWT工具类
     */
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 用户详情服务
     */
    @Autowired
    private UserDetailsServiceImpl userDetailsService;
    
    /**
     * JWT头部名称
     */
    @Value("${jwt.header}")
    private String jwtHeader;
    
    /**
     * JWT前缀
     */
    @Value("${jwt.prefix}")
    private String jwtPrefix;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中解析JWT令牌
     * 
     * @param request HTTP请求
     * @return JWT令牌
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(jwtHeader);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(jwtPrefix)) {
            return headerAuth.substring(jwtPrefix.length()).trim();
        }

        return null;
    }
} 