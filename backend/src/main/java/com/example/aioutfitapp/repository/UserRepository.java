package com.example.aioutfitapp.repository;

import com.example.aioutfitapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓库接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 通过用户名查找用户
     * 
     * @param username 用户名
     * @return 用户对象
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 通过电子邮件查找用户
     * 
     * @param email 电子邮件
     * @return 用户对象
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     * 
     * @param username 用户名
     * @return 是否存在
     */
    Boolean existsByUsername(String username);
    
    /**
     * 检查电子邮件是否存在
     * 
     * @param email 电子邮件
     * @return 是否存在
     */
    Boolean existsByEmail(String email);
} 