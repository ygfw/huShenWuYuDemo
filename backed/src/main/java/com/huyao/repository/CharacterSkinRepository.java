package com.huyao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.CharacterSkin;

public interface CharacterSkinRepository extends JpaRepository<CharacterSkin, Long> {
    List<CharacterSkin> findByCharacterId(Long characterId);

    Optional<CharacterSkin> findByCharacterIdAndSkinKey(Long characterId, String skinKey);
}
