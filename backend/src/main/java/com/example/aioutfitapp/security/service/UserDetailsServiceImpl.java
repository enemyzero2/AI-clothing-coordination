package com.example.aioutfitapp.security.service;

import com.example.aioutfitapp.model.User;
import com.example.aioutfitapp.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户详情服务实现类
 * 
 * 实现Spring Security的UserDetailsService接口
 */
@Service
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    
    /**
     * 用户仓库
     */
    @Autowired
    UserRepository userRepository;

    /**
     * 根据用户名加载用户详情
     * 
     * @param username 用户名
     * @return 用户详情
     * @throws UsernameNotFoundException 用户名未找到异常
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new UsernameNotFoundException("未找到用户: " + username);
                });

        return UserDetailsImpl.build(user);
    }
} 