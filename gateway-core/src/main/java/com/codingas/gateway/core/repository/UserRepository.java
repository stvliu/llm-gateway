package com.codingas.gateway.core.repository;

import com.codingas.gateway.core.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User 仓储接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户编码查询
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * 根据邮箱查询
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名查询
     */
    Optional<User> findByUsername(String username);
}
