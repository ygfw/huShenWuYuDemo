package com.huyao.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.Character;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {
  List<Character> findByNameIn(List<String> names);

  java.util.Optional<Character> findByName(String name);
}
