package com.huyao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
