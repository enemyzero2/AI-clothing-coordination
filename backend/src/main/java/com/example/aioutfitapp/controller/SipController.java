package com.example.aioutfitapp.controller;

import com.example.aioutfitapp.model.SipUser;
import com.example.aioutfitapp.security.service.UserDetailsImpl;
import com.example.aioutfitapp.service.SipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SIP控制器
 * 
 * 处理SIP相关的HTTP请求
 */
@RestController
@RequestMapping("/sip")
@Slf4j
public class SipController {

    /**
     * SIP服务
     */
    @Autowired
    private SipService sipService;
    
    /**
     * SIP服务器地址
     */
    @Value("${sip.server.address:localhost}")
    private String sipServerAddress;
    
    /**
     * SIP服务器端口
     */
    @Value("${sip.server.port:5060}")
    private String sipServerPort;
    
    /**
     * SIP域名
     */
    @Value("${sip.domain:localhost}")
    private String sipDomain;

    /**
     * 获取当前用户的SIP账户信息
     * 
     * @return SIP账户信息
     */
    @GetMapping("/account")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSipAccount() {
        try {
            // 获取当前认证用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();
            
            // 获取或创建SIP账户
            SipUser sipUser = sipService.createSipAccount(userId);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sipUsername", sipUser.getSipUsername());
            response.put("sipPassword", sipUser.getSipPassword());
            response.put("sipDomain", sipUser.getDomain());
            response.put("sipServerAddress", sipServerAddress);
            response.put("sipServerPort", sipServerPort);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取SIP账户信息失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取SIP账户信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取当前用户的所有SIP账户
     * 
     * @return SIP账户列表
     */
    @GetMapping("/accounts")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<?> getSipAccounts() {
        try {
            // 获取当前认证用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();
            
            // 获取SIP账户列表
            List<SipUser> sipUsers = sipService.findByUserId(userId);
            
            // 转换为响应格式
            List<Map<String, Object>> accounts = sipUsers.stream().map(sipUser -> {
                Map<String, Object> account = new HashMap<>();
                account.put("id", sipUser.getId());
                account.put("sipUsername", sipUser.getSipUsername());
                account.put("sipPassword", sipUser.getSipPassword());
                account.put("sipDomain", sipUser.getDomain());
                account.put("isActive", sipUser.getIsActive());
                account.put("createTime", sipUser.getCreateTime());
                return account;
            }).collect(Collectors.toList());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("accounts", accounts);
            response.put("sipServerAddress", sipServerAddress);
            response.put("sipServerPort", sipServerPort);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取SIP账户列表失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取SIP账户列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 