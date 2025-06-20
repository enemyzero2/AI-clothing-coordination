package com.example.aioutfitapp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SIP用户实体类
 * 
 * 存储SIP账户信息
 */
@Entity
@Table(name = "sip_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SipUser {
    
    /**
     * SIP用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID（关联到User表）
     */
    @Column(name = "user_id")
    private String userId;
    
    /**
     * SIP账户名称
     */
    @NotBlank
    @Size(max = 50)
    @Column(name = "sip_username", unique = true)
    private String sipUsername;
    
    /**
     * SIP密码
     */
    @NotBlank
    @Size(max = 128)
    @Column(name = "sip_password")
    private String sipPassword;
    
    /**
     * 域名
     */
    @NotBlank
    @Size(max = 100)
    @Column(name = "domain")
    private String domain;
    
    /**
     * 是否活跃
     */
    @Column(name = "is_active")
    private Boolean isActive;
    
    /**
     * 创建时间（时间戳）
     */
    @Column(name = "create_time")
    private Long createTime;
    
    /**
     * 更新时间（时间戳）
     */
    @Column(name = "update_time")
    private Long updateTime;

    /**
     * 用户引用
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
} 