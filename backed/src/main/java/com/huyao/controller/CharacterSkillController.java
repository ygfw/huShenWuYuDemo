package com.huyao.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.huyao.entity.Character;
import com.huyao.entity.CharacterSkill;
import com.huyao.repository.CharacterRepository;
import com.huyao.repository.CharacterSkillRepository;

@RestController
public class CharacterSkillController {
    private static final Pattern SKILL_PATTERN = Pattern
            .compile("^(character\\d+)_(bigSkill|skill1|skill2)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> KEY_MAP = Map.of(
            "bigSkill", "ultimate",
            "skill1", "skill1",
            "skill2", "skill2");

    private final CharacterRepository characterRepository;
    private final CharacterSkillRepository characterSkillRepository;

    public CharacterSkillController(CharacterRepository characterRepository,
            CharacterSkillRepository characterSkillRepository) {
        this.characterRepository = characterRepository;
        this.characterSkillRepository = characterSkillRepository;
    }

    @GetMapping("/api/character-skills")
    public List<CharacterSkillResponse> listSkills(@RequestParam("characterId") String characterId) {
        Long id;
        try {
            id = Long.parseLong(characterId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid characterId");
        }
        syncSkillsFromFiles();
        return characterSkillRepository.findByCharacterId(id).stream()
                .map(CharacterSkillResponse::from)
                .toList();
    }

    private void syncSkillsFromFiles() {
        listSkillFiles().forEach(filename -> {
            Matcher matcher = SKILL_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                return;
            }
            String characterName = matcher.group(1);
            String rawKey = matcher.group(2);
            String skillKey = KEY_MAP.get(rawKey);
            if (skillKey == null) {
                return;
            }
            Character character = characterRepository.findByName(characterName).orElse(null);
            if (character == null) {
                return;
            }
            String imagePath = "/images/skills/" + filename;
            CharacterSkill skill = characterSkillRepository
                    .findByCharacterIdAndSkillKey(character.getId(), skillKey)
                    .orElseGet(() -> {
                        CharacterSkill created = new CharacterSkill();
                        created.setCharacterId(character.getId());
                        created.setSkillKey(skillKey);
                        return created;
                    });
            if (!imagePath.equals(skill.getImage())) {
                skill.setImage(imagePath);
                characterSkillRepository.save(skill);
            }
        });
    }

    private List<String> listSkillFiles() {
        Path rootImages = Paths.get("..", "images", "skills").toAbsolutePath().normalize();
        Path frontedImages = Paths.get("..", "fronted", "images", "skills").toAbsolutePath().normalize();

        return Stream.of(rootImages, frontedImages)
                .filter(Files::exists)
                .flatMap(path -> {
                    try {
                        return Files.list(path);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .map(path -> path.getFileName().toString())
                .filter(name -> SKILL_PATTERN.matcher(name).matches())
                .distinct()
                .toList();
    }

    public record CharacterSkillResponse(
            String id,
            String characterId,
            String skillKey,
            String image) {
        public static CharacterSkillResponse from(CharacterSkill skill) {
            return new CharacterSkillResponse(
                    String.valueOf(skill.getId()),
                    String.valueOf(skill.getCharacterId()),
                    skill.getSkillKey(),
                    skill.getImage());
        }
    }
}
