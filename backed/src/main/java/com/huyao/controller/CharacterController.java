package com.huyao.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.huyao.entity.Character;
import com.huyao.entity.CharacterSkin;
import com.huyao.entity.HomeState;
import com.huyao.repository.CharacterRepository;
import com.huyao.repository.CharacterSkinRepository;
import com.huyao.repository.HomeStateRepository;

@RestController
public class CharacterController {
    private final CharacterRepository characterRepository;
    private final CharacterSkinRepository characterSkinRepository;
    private final HomeStateRepository homeStateRepository;

    public CharacterController(CharacterRepository characterRepository,
            CharacterSkinRepository characterSkinRepository,
            HomeStateRepository homeStateRepository) {
        this.characterRepository = characterRepository;
        this.characterSkinRepository = characterSkinRepository;
        this.homeStateRepository = homeStateRepository;
    }

    @GetMapping("/api/characters")
    public List<CharacterResponse> listCharacters() {
        syncCharactersFromFiles();
        syncHomeStateQuote();
        return characterRepository.findAll().stream()
                .map(CharacterResponse::from)
                .toList();
    }

    @PostMapping("/api/characters")
    public CharacterResponse createCharacter(@RequestBody CharacterRequest request) {
        Character character = new Character();
        character.setName(request.name());
        character.setImage(request.image());
        character.setUltimateDesc(request.ultimateDesc());
        character.setSkill1Desc(request.skill1Desc());
        character.setSkill2Desc(request.skill2Desc());
        character.setQuote(request.quote());
        character.setObtainedAt(request.obtainedAt());
        return CharacterResponse.from(characterRepository.save(character));
    }

    @PostMapping("/api/characters/batch")
    public List<CharacterResponse> createCharacters(@RequestBody List<CharacterRequest> requests) {
        List<Character> entities = requests.stream().map(request -> {
            Character character = new Character();
            character.setName(request.name());
            character.setImage(request.image());
            character.setUltimateDesc(request.ultimateDesc());
            character.setSkill1Desc(request.skill1Desc());
            character.setSkill2Desc(request.skill2Desc());
            character.setQuote(request.quote());
            character.setObtainedAt(request.obtainedAt());
            return character;
        }).toList();

        return characterRepository.saveAll(entities).stream()
                .map(CharacterResponse::from)
                .toList();
    }

    @GetMapping("/api/home-character")
    public HomeStateResponse getHomeCharacter() {
        return homeStateRepository.findById(1L)
                .map(state -> {
                    ensureHomeSkin(state);
                    return HomeStateResponse.from(state, resolveSkinImage(state));
                })
                .orElse(null);
    }

    @PostMapping("/api/home-character")
    public HomeStateResponse setHomeCharacter(@RequestBody HomeStateRequest request) {
        if (request.characterId() == null || request.characterId().isBlank()) {
            throw new IllegalArgumentException("characterId required");
        }
        Long id = Long.parseLong(request.characterId());
        Character character = characterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "character not found"));
        HomeState state = homeStateRepository.findById(1L).orElse(new HomeState());
        state.setCharacterId(id);
        Long skinId = resolveSkinId(request.skinId(), id);
        if (skinId == null) {
            skinId = findDefaultSkinId(id);
        }
        state.setSkinId(skinId);
        state.setQuote(character.getQuote());
        HomeState saved = homeStateRepository.save(state);
        return HomeStateResponse.from(saved, resolveSkinImage(saved));
    }

    private void syncCharactersFromFiles() {
        Set<String> files = listCharacterFiles().stream()
                .collect(Collectors.toSet());

        if (files.isEmpty()) {
            return;
        }

        for (String filename : files) {
            String name = filename.replaceAll("\\.(jpg|jpeg|png)$", "");
            String imagePath = "/images/character/" + filename;
            var existing = characterRepository.findByName(name);
            if (existing.isPresent()) {
                Character character = existing.get();
                if (!imagePath.equals(character.getImage())) {
                    character.setImage(imagePath);
                    characterRepository.save(character);
                }
                continue;
            }
            Character character = new Character();
            character.setName(name);
            character.setImage(imagePath);
            characterRepository.save(character);
        }
    }

    private void syncHomeStateQuote() {
        homeStateRepository.findById(1L).ifPresent(state -> {
            Long characterId = state.getCharacterId();
            if (characterId == null) {
                return;
            }
            characterRepository.findById(characterId).ifPresent(character -> {
                String latestQuote = character.getQuote();
                if (latestQuote == null) {
                    return;
                }
                if (!latestQuote.equals(state.getQuote())) {
                    state.setQuote(latestQuote);
                    homeStateRepository.save(state);
                }
            });
        });
    }

    private Long resolveSkinId(String skinIdValue, Long characterId) {
        if (skinIdValue == null || skinIdValue.isBlank()) {
            return null;
        }
        try {
            Long skinId = Long.parseLong(skinIdValue);
            CharacterSkin skin = characterSkinRepository.findById(skinId).orElse(null);
            if (skin == null || !characterId.equals(skin.getCharacterId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid skin");
            }
            return skinId;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid skin");
        }
    }

    private String resolveSkinImage(HomeState state) {
        Long skinId = state.getSkinId();
        if (skinId == null) {
            return null;
        }
        return characterSkinRepository.findById(skinId)
                .map(CharacterSkin::getImage)
                .orElse(null);
    }

    private void ensureHomeSkin(HomeState state) {
        if (state.getCharacterId() == null || state.getSkinId() != null) {
            return;
        }
        Long defaultSkinId = findDefaultSkinId(state.getCharacterId());
        if (defaultSkinId != null) {
            state.setSkinId(defaultSkinId);
            homeStateRepository.save(state);
        }
    }

    private Long findDefaultSkinId(Long characterId) {
        return characterSkinRepository.findByCharacterIdAndSkinKey(characterId, "default")
                .map(CharacterSkin::getId)
                .orElse(null);
    }

    private List<String> listCharacterFiles() {
        Path rootImages = Paths.get("..", "images", "character").toAbsolutePath().normalize();
        Path frontedImages = Paths.get("..", "fronted", "images", "character").toAbsolutePath().normalize();
        Pattern pattern = Pattern.compile("^character\\d+\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);

        return Stream.of(rootImages, frontedImages)
                .filter(Files::exists)
                .flatMap(path -> {
                    try {
                        return Files.list(path);
                    } catch (IOException e) {
                        return Stream.empty();
                    }
                })
                .map(p -> p.getFileName().toString())
                .filter(name -> pattern.matcher(name).matches())
                .distinct()
                .toList();
    }

    public record CharacterRequest(
            String name,
            String image,
            String ultimateDesc,
            String skill1Desc,
            String skill2Desc,
            String quote,
            java.time.LocalDateTime obtainedAt) {
    }

    public record CharacterResponse(
            String id,
            String name,
            String image,
            String ultimateDesc,
            String skill1Desc,
            String skill2Desc,
            String quote,
            java.time.LocalDateTime obtainedAt) {
        public static CharacterResponse from(Character character) {
            return new CharacterResponse(
                    String.valueOf(character.getId()),
                    character.getName(),
                    character.getImage(),
                    character.getUltimateDesc(),
                    character.getSkill1Desc(),
                    character.getSkill2Desc(),
                    character.getQuote(),
                    character.getObtainedAt());
        }
    }

    public record HomeStateRequest(String characterId, String skinId) {
    }

    public record HomeStateResponse(String characterId, String quote, String skinId, String skinImage) {
        public static HomeStateResponse from(HomeState state, String skinImage) {
            return new HomeStateResponse(
                    String.valueOf(state.getCharacterId()),
                    state.getQuote(),
                    state.getSkinId() == null ? null : String.valueOf(state.getSkinId()),
                    skinImage);
        }
    }
}
