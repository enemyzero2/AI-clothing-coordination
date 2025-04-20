package com.example.aioutfitapp.repository;

import com.example.aioutfitapp.model.Role;
import com.example.aioutfitapp.model.Role.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色仓库接口
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    /**
     * 通过角色名称查找角色
     * 
     * @param name 角色名称
     * @return 角色对象
     */
    Optional<Role> findByName(ERole name);
} 