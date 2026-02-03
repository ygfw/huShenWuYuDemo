package com.huyao.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import com.huyao.entity.User;
import com.huyao.repository.UserRepository;

@RestController
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users")
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/api/users/{id}")
    public UserResponse getUser(@PathVariable("id") String id, HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(id);
            Object authUserId = request.getAttribute("authUserId");
            if (authUserId != null && !String.valueOf(authUserId).equals(id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden");
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
            return UserResponse.from(user);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid userId");
        }
    }

    @PostMapping("/api/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setPhonenumber(request.phonenumber());
        return UserResponse.from(userRepository.save(user));
    }

    public record UserCreateRequest(String username, String password, String phonenumber) {
    }

    public record UserResponse(String id, String username, String phonenumber, String createdAt,
            Integer level, Integer drawCount) {
        public static UserResponse from(User user) {
            return new UserResponse(
                    String.valueOf(user.getId()),
                    user.getUsername(),
                    user.getPhonenumber(),
                    user.getCreatedAt().toString(),
                    user.getLevel(),
                    user.getDrawCount());
        }
    }
}
