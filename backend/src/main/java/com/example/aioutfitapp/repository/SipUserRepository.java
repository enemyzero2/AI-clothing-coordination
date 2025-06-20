package com.example.aioutfitapp.repository;

import com.example.aioutfitapp.model.SipUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SIP用户仓库
 */
@Repository
public interface SipUserRepository extends JpaRepository<SipUser, Long> {
    
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
     * 检查SIP用户名是否存在
     * 
     * @param sipUsername SIP用户名
     * @return 是否存在
     */
    boolean existsBySipUsername(String sipUsername);
    
    /**
     * 根据域名查询SIP用户列表
     * 
     * @param domain 域名
     * @return SIP用户列表
     */
    List<SipUser> findByDomain(String domain);
} 