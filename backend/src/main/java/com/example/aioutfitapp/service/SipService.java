package com.example.aioutfitapp.service;

import com.example.aioutfitapp.model.SipUser;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * SIP服务接口
 */
public interface SipService {
    
    /**
     * 为用户创建SIP账户
     * 
     * @param userId 用户ID
     * @return SIP用户
     */
    SipUser createSipAccount(String userId);
    
    /**
     * 根据SIP用户名查找SIP用户
     * 
     * @param sipUsername SIP用户名
     * @return SIP用户
     */
    Optional<SipUser> findBySipUsername(String sipUsername);
    
    /**
     * 根据用户ID查找SIP用户列表
     * 
     * @param userId 用户ID
     * @return SIP用户列表
     */
    List<SipUser> findByUserId(String userId);
    
    /**
     * 处理FreeSWITCH directory请求
     * 
     * @param section section参数
     * @param key_value key_value参数
     * @param key key参数
     * @param user 用户名
     * @param domain 域名
     * @return XML响应
     */
    String handleDirectoryRequest(String section, String key_value, String key, String user, String domain);
    
    /**
     * 处理FreeSWITCH configuration请求
     * 
     * @param section section参数
     * @param key_value key_value参数
     * @param key key参数
     * @return XML响应
     */
    String handleConfigurationRequest(String section, String key_value, String key);
    
    /**
     * 处理FreeSWITCH dialplan请求
     * 
     * @param section section参数
     * @param context context参数
     * @param destinationNumber 目标号码
     * @return XML响应
     */
    String handleDialplanRequest(String section, String context, String destinationNumber);
} 