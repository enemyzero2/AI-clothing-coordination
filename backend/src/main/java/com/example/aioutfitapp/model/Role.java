package com.example.aioutfitapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * 用户角色实体类
 */
@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    /**
     * 角色ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    /**
     * 角色名称
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;
    
    /**
     * 角色名称枚举
     */
    public enum ERole {
        /**
         * 普通用户
         */
        ROLE_USER,
        
        /**
         * 管理员
         */
        ROLE_ADMIN
    }
} 