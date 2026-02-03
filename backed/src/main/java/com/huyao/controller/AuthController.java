package com.huyao.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.huyao.entity.Character;
import com.huyao.entity.HomeState;
import com.huyao.entity.User;
import com.huyao.entity.UserCharacter;
import com.huyao.repository.CharacterRepository;
import com.huyao.repository.HomeStateRepository;
import com.huyao.repository.UserCharacterRepository;
import com.huyao.repository.UserRepository;
import com.huyao.util.JwtUtil;

@RestController
public class AuthController {
    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final HomeStateRepository homeStateRepository;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
            CharacterRepository characterRepository,
            UserCharacterRepository userCharacterRepository,
            HomeStateRepository homeStateRepository,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.characterRepository = characterRepository;
        this.userCharacterRepository = userCharacterRepository;
        this.homeStateRepository = homeStateRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.phonenumber() == null || request.phonenumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少必要字段");
        }

        Optional<User> existing = userRepository.findByUsername(request.username());
        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPassword(hash(request.password()));
        user.setPhonenumber(request.phonenumber().trim());
        User saved = userRepository.save(user);
        ensureStarterCharacter(saved);
        return AuthResponse.from(saved, jwtUtil.generateToken(saved));
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少必要字段");
        }

        User user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!hash(request.password()).equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        return AuthResponse.from(user, jwtUtil.generateToken(user));
    }

    public record RegisterRequest(String username, String password, String phonenumber) {
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuthResponse(UserController.UserResponse user, String token) {
        public static AuthResponse from(User user, String token) {
            return new AuthResponse(UserController.UserResponse.from(user), token);
        }
    }

    private void ensureStarterCharacter(User user) {
        Character starter = characterRepository.findByName("character1").orElse(null);
        if (starter == null) {
            return;
        }
        userCharacterRepository.findByUserIdAndCharacterId(user.getId(), starter.getId())
                .orElseGet(() -> {
                    UserCharacter owned = new UserCharacter();
                    owned.setUserId(user.getId());
                    owned.setCharacterId(starter.getId());
                    owned.setDuplicateCount(0);
                    return userCharacterRepository.save(owned);
                });

        homeStateRepository.findById(1L).orElseGet(() -> {
            HomeState state = new HomeState();
            state.setCharacterId(starter.getId());
            state.setQuote(starter.getQuote());
            return homeStateRepository.save(state);
        });
    }

    private String hash(String input) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("hash error");
        }
    }
}
