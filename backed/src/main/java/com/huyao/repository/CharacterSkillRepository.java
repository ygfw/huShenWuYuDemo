package com.huyao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.CharacterSkill;

public interface CharacterSkillRepository extends JpaRepository<CharacterSkill, Long> {
    List<CharacterSkill> findByCharacterId(Long characterId);

    Optional<CharacterSkill> findByCharacterIdAndSkillKey(Long characterId, String skillKey);
}
