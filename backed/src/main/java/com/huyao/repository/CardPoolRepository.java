package com.huyao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.CardPool;

public interface CardPoolRepository extends JpaRepository<CardPool, Long> {
    Optional<CardPool> findByPoolKey(String poolKey);
}
