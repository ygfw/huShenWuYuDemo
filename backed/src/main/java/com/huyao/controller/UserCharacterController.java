package com.huyao.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.huyao.entity.Character;
import com.huyao.entity.User;
import com.huyao.entity.UserCharacter;
import com.huyao.repository.CharacterRepository;
import com.huyao.repository.UserCharacterRepository;
import com.huyao.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class UserCharacterController {
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public UserCharacterController(UserCharacterRepository userCharacterRepository,
            CharacterRepository characterRepository,
            UserRepository userRepository) {
        this.userCharacterRepository = userCharacterRepository;
        this.characterRepository = characterRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/api/user-characters")
    public List<UserCharacterResponse> listUserCharacters(
            @RequestParam(value = "userId", required = false) String userId,
            HttpServletRequest request) {
        Long uid = resolveUserId(userId, request);
        return userCharacterRepository.findByUserId(uid).stream()
                .map(UserCharacterResponse::from)
                .toList();
    }

    @PostMapping("/api/user-characters/draw")
    @ResponseStatus(HttpStatus.CREATED)
    public DrawResponse drawCharacter(@RequestBody DrawRequest request, HttpServletRequest httpRequest) {
        Long uid = resolveUserId(request.userId(), httpRequest);
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户不存在"));
        List<Character> pool = characterRepository.findAll();
        if (pool.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色池为空");
        }
        updateDrawProgress(user, 1);

        double totalChance = Math.min(1.0, pool.size() * 0.05);
        if (random.nextDouble() > totalChance) {
            return DrawResponse.empty();
        }

        Character picked = pool.get(random.nextInt(pool.size()));
        UserCharacter owned = userCharacterRepository
                .findByUserIdAndCharacterId(uid, picked.getId())
                .orElse(null);

        if (owned == null) {
            owned = new UserCharacter();
            owned.setUserId(uid);
            owned.setCharacterId(picked.getId());
            owned.setDuplicateCount(0);
        } else {
            owned.setDuplicateCount(owned.getDuplicateCount() + 1);
        }

        userCharacterRepository.save(owned);

        return DrawResponse.of(picked, owned);
    }

    @PostMapping("/api/user-characters/bootstrap")
    public List<UserCharacterResponse> bootstrapCharacters(@RequestBody BootstrapRequest request,
            HttpServletRequest httpRequest) {
        Long uid = resolveUserId(request.userId(), httpRequest);
        List<String> names = request.names();
        List<Character> candidates;
        if (names != null && !names.isEmpty()) {
            candidates = characterRepository.findByNameIn(names);
        } else {
            candidates = characterRepository.findByNameIn(List.of("character1"));
        }

        List<UserCharacter> existing = userCharacterRepository.findByUserId(uid);
        var existingIds = existing.stream()
                .map(UserCharacter::getCharacterId)
                .collect(Collectors.toSet());

        List<UserCharacter> toSave = candidates.stream()
                .filter(c -> !existingIds.contains(c.getId()))
                .map(c -> {
                    UserCharacter uc = new UserCharacter();
                    uc.setUserId(uid);
                    uc.setCharacterId(c.getId());
                    uc.setDuplicateCount(0);
                    uc.setObtainedAt(LocalDateTime.now());
                    return uc;
                })
                .toList();

        if (!toSave.isEmpty()) {
            userCharacterRepository.saveAll(toSave);
        }

        return userCharacterRepository.findByUserId(uid).stream()
                .map(UserCharacterResponse::from)
                .toList();
    }

    private Long resolveUserId(String value, HttpServletRequest request) {
        Object authUserId = request.getAttribute("authUserId");
        String tokenUserId = authUserId == null ? null : String.valueOf(authUserId);
        if (tokenUserId != null && value != null && !tokenUserId.equals(value)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
        }
        String finalId = tokenUserId != null ? tokenUserId : value;
        if (finalId == null || finalId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId required");
        }
        try {
            return Long.parseLong(finalId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid userId");
        }
    }

    private void updateDrawProgress(User user, int draws) {
        int current = user.getDrawCount() == null ? 0 : user.getDrawCount();
        int updated = current + Math.max(0, draws);
        user.setDrawCount(updated);
        user.setLevel(calculateLevel(updated));
        userRepository.save(user);
    }

    private int calculateLevel(int totalDraws) {
        if (totalDraws <= 0) {
            return 1;
        }
        for (int level = 1; level <= 100; level++) {
            long required = 5L * (level - 1) * level;
            if (totalDraws < required) {
                return Math.max(1, level - 1);
            }
        }
        return 100;
    }

    public record DrawRequest(String userId) {
    }

    public record BootstrapRequest(String userId, List<String> names) {
    }

    public record UserCharacterResponse(String id, String userId, String characterId, Integer duplicateCount,
            java.time.LocalDateTime obtainedAt) {
        public static UserCharacterResponse from(UserCharacter entity) {
            return new UserCharacterResponse(
                    String.valueOf(entity.getId()),
                    String.valueOf(entity.getUserId()),
                    String.valueOf(entity.getCharacterId()),
                    entity.getDuplicateCount(),
                    entity.getObtainedAt());
        }
    }

    public record DrawResponse(Boolean hit,
            CharacterController.CharacterResponse character,
            UserCharacterResponse owned) {
        public static DrawResponse of(Character character, UserCharacter owned) {
            return new DrawResponse(
                    true,
                    CharacterController.CharacterResponse.from(character),
                    UserCharacterResponse.from(owned));
        }

        public static DrawResponse empty() {
            return new DrawResponse(false, null, null);
        }
    }
}
