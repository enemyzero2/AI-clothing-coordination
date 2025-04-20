package com.example.aioutfitapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类
 * 
 * 存储用户基本信息
 */
@Entity
@Table(name = "user", 
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    
    /**
     * 用户名
     */
    @NotBlank
    @Size(max = 50)
    @Column(name = "username")
    private String username;
    
    /**
     * 电子邮箱
     */
    @NotBlank
    @Size(max = 100)
    @Email
    @Column(name = "email")
    private String email;
    
    /**
     * 密码哈希
     */
    @NotBlank
    @Size(max = 128)
    @Column(name = "password_hash")
    private String passwordHash;
    
    /**
     * 头像URI
     */
    @Size(max = 255)
    @Column(name = "avatar_uri")
    private String avatarUri;
    
    /**
     * 显示名称
     */
    @Size(max = 100)
    @Column(name = "display_name")
    private String displayName;
    
    /**
     * 个人简介
     */
    @Column(name = "bio")
    private String bio;
    
    /**
     * 注册日期（时间戳）
     */
    @Column(name = "registration_date")
    private Long registrationDate;
    
    /**
     * 上次登录日期（时间戳）
     */
    @Column(name = "last_login_date")
    private Long lastLoginDate;
    
    /**
     * 认证令牌
     */
    @Column(name = "token")
    private String token;
    
    /**
     * 是否活跃
     */
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * 用户角色
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", 
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
} 