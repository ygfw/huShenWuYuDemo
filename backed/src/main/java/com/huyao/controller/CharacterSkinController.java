package com.huyao.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.huyao.entity.Character;
import com.huyao.entity.CharacterSkin;
import com.huyao.repository.CharacterRepository;
import com.huyao.repository.CharacterSkinRepository;

@RestController
public class CharacterSkinController {
    private static final Pattern SKIN_PATTERN = Pattern
            .compile("^(character\\d+)_skin(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);

    private final CharacterRepository characterRepository;
    private final CharacterSkinRepository characterSkinRepository;

    public CharacterSkinController(CharacterRepository characterRepository,
            CharacterSkinRepository characterSkinRepository) {
        this.characterRepository = characterRepository;
        this.characterSkinRepository = characterSkinRepository;
    }

    @GetMapping("/api/character-skins")
    public List<CharacterSkinResponse> listSkins(@RequestParam("characterId") String characterId) {
        Long id;
        try {
            id = Long.parseLong(characterId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid characterId");
        }
        syncDefaultSkins();
        syncSkinsFromFiles();
        return characterSkinRepository.findByCharacterId(id).stream()
                .map(CharacterSkinResponse::from)
                .toList();
    }

    private void syncDefaultSkins() {
        characterRepository.findAll().forEach(character -> {
            CharacterSkin skin = characterSkinRepository
                    .findByCharacterIdAndSkinKey(character.getId(), "default")
                    .orElseGet(() -> {
                        CharacterSkin created = new CharacterSkin();
                        created.setCharacterId(character.getId());
                        created.setSkinKey("default");
                        created.setIsDefault(true);
                        return created;
                    });
            boolean dirty = false;
            if (!"原皮".equals(skin.getName())) {
                skin.setName("原皮");
                dirty = true;
            }
            if (!character.getImage().equals(skin.getImage())) {
                skin.setImage(character.getImage());
                dirty = true;
            }
            if (skin.getIsDefault() == null || !skin.getIsDefault()) {
                skin.setIsDefault(true);
                dirty = true;
            }
            if (dirty) {
                characterSkinRepository.save(skin);
            }
        });
    }

    private void syncSkinsFromFiles() {
        listSkinFiles().forEach(filename -> {
            Matcher matcher = SKIN_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                return;
            }
            String characterName = matcher.group(1);
            String skinIndex = matcher.group(2);
            Character character = characterRepository.findByName(characterName).orElse(null);
            if (character == null) {
                return;
            }
            String skinKey = "skin" + skinIndex;
            String imagePath = "/images/character_skins/" + filename;
            CharacterSkin skin = characterSkinRepository
                    .findByCharacterIdAndSkinKey(character.getId(), skinKey)
                    .orElseGet(() -> {
                        CharacterSkin created = new CharacterSkin();
                        created.setCharacterId(character.getId());
                        created.setSkinKey(skinKey);
                        created.setIsDefault(false);
                        return created;
                    });
            boolean dirty = false;
            String expectedName = "皮肤" + skinIndex;
            if (!expectedName.equals(skin.getName())) {
                skin.setName(expectedName);
                dirty = true;
            }
            if (!imagePath.equals(skin.getImage())) {
                skin.setImage(imagePath);
                dirty = true;
            }
            if (skin.getIsDefault() == null || skin.getIsDefault()) {
                skin.setIsDefault(false);
                dirty = true;
            }
            if (dirty) {
                characterSkinRepository.save(skin);
            }
        });
    }

    private List<String> listSkinFiles() {
        Path rootImages = Paths.get("..", "images", "character_skins").toAbsolutePath().normalize();
        Path frontedImages = Paths.get("..", "fronted", "images", "character_skins").toAbsolutePath().normalize();

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
                .filter(name -> SKIN_PATTERN.matcher(name).matches())
                .distinct()
                .toList();
    }

    public record CharacterSkinResponse(
            String id,
            String characterId,
            String name,
            String image,
            String skinKey,
            Boolean isDefault) {
        public static CharacterSkinResponse from(CharacterSkin skin) {
            return new CharacterSkinResponse(
                    String.valueOf(skin.getId()),
                    String.valueOf(skin.getCharacterId()),
                    skin.getName(),
                    skin.getImage(),
                    skin.getSkinKey(),
                    skin.getIsDefault());
        }
    }
}
