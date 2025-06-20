package com.example.aioutfitapp.service.impl;

import com.example.aioutfitapp.dto.request.LoginRequest;
import com.example.aioutfitapp.dto.request.RegisterRequest;
import com.example.aioutfitapp.dto.response.LoginResponse;
import com.example.aioutfitapp.dto.response.RegisterResponse;
import com.example.aioutfitapp.model.Role;
import com.example.aioutfitapp.model.Role.ERole;
import com.example.aioutfitapp.model.SipUser;
import com.example.aioutfitapp.model.User;
import com.example.aioutfitapp.repository.RoleRepository;
import com.example.aioutfitapp.repository.UserRepository;
import com.example.aioutfitapp.security.jwt.JwtUtils;
import com.example.aioutfitapp.security.service.UserDetailsImpl;
import com.example.aioutfitapp.service.AuthService;
import com.example.aioutfitapp.service.SipService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    /**
     * 用户仓库
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 角色仓库
     */
    @Autowired
    private RoleRepository roleRepository;

    /**
     * 认证管理器
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 密码编码器
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * JWT工具类
     */
    @Autowired
    private JwtUtils jwtUtils;
    
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
     * 用户登录
     * 
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // 验证用户名和密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            // 设置认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 生成JWT令牌
            String jwt = jwtUtils.generateJwtToken(authentication);

            // 获取用户详情
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            // 获取用户角色
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            // 更新用户最后登录时间
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            SipUser sipUser = null;
            
            if (user != null) {
                user.setLastLoginDate(System.currentTimeMillis());
                user.setToken(jwt);
                userRepository.save(user);
                
                // 为用户创建或获取SIP账户
                try {
                    sipUser = sipService.createSipAccount(user.getId());
                    log.info("为用户 {} 创建或获取SIP账户成功: {}", user.getUsername(), sipUser.getSipUsername());
                } catch (Exception e) {
                    log.error("为用户 {} 创建SIP账户失败", user.getUsername(), e);
                }
            }
            
            // 构建SIP账户信息
            LoginResponse.SipAccount sipAccount = null;
            if (sipUser != null) {
                sipAccount = LoginResponse.SipAccount.builder()
                    .sipUsername(sipUser.getSipUsername())
                    .sipPassword(sipUser.getSipPassword())
                    .sipDomain(sipUser.getDomain())
                    .sipServerAddress(sipServerAddress)
                    .sipServerPort(sipServerPort)
                    .build();
            }

            // 构建用户详情对象（兼容APP端）
            LoginResponse.UserDetail userDetail = LoginResponse.UserDetail.builder()
                .id(user != null ? Integer.valueOf(user.getId().hashCode()) : null)
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .avatar(userDetails.getAvatarUri())
                .roles(roles)
                .sipAccount(sipAccount)
                .build();

            // 返回登录响应
            return LoginResponse.builder()
                    .success(true)
                    .message("登录成功")
                    .token(jwt)
                    .tokenType("Bearer")
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .roles(roles)
                    .avatarUri(userDetails.getAvatarUri())
                    .displayName(userDetails.getDisplayName())
                    .sipAccount(sipAccount)
                    .user(userDetail) // 添加用户详情对象（兼容APP端）
                    .build();
        } catch (Exception e) {
            log.error("登录失败", e);
            return LoginResponse.builder()
                    .success(false)
                    .message("登录失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 用户注册
     * 
     * @param registerRequest 注册请求
     * @return 注册响应
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        try {
            // 检查用户名是否存在
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                return RegisterResponse.builder()
                        .success(false)
                        .message("用户名已存在")
                        .build();
            }

            // 检查邮箱是否存在
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                return RegisterResponse.builder()
                        .success(false)
                        .message("邮箱已被使用")
                        .build();
            }

            // 创建新用户对象
            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                    .registrationDate(System.currentTimeMillis())
                    .isActive(true)
                    .displayName(registerRequest.getUsername())
                    .build();

            // 设置用户角色
            Set<Role> roles = new HashSet<>();
            
            // 默认设置为普通用户角色
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("未找到角色: " + ERole.ROLE_USER));
            roles.add(userRole);
            
            user.setRoles(roles);
            
            // 保存用户
            User savedUser = userRepository.save(user);
            
            // 为新注册用户创建SIP账户
            try {
                SipUser sipUser = sipService.createSipAccount(savedUser.getId());
                log.info("为新注册用户 {} 创建SIP账户成功: {}", savedUser.getUsername(), sipUser.getSipUsername());
            } catch (Exception e) {
                log.error("为新注册用户 {} 创建SIP账户失败", savedUser.getUsername(), e);
                // 继续处理，不影响注册流程
            }

            // 返回注册响应
            return RegisterResponse.builder()
                    .success(true)
                    .message("注册成功")
                    .userId(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .build();
        } catch (Exception e) {
            log.error("注册失败", e);
            return RegisterResponse.builder()
                    .success(false)
                    .message("注册失败: " + e.getMessage())
                    .build();
        }
    }
} 