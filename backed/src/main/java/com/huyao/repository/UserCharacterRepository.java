package com.huyao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.UserCharacter;

public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
    List<UserCharacter> findByUserId(Long userId);

    Optional<UserCharacter> findByUserIdAndCharacterId(Long userId, Long characterId);
}
