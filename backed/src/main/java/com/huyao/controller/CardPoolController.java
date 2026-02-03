package com.huyao.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huyao.entity.CardPool;
import com.huyao.repository.CardPoolRepository;

@RestController
public class CardPoolController {
    private static final Pattern POOL_PATTERN = Pattern
            .compile("^cardPoolBackground(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);

    private final CardPoolRepository cardPoolRepository;

    public CardPoolController(CardPoolRepository cardPoolRepository) {
        this.cardPoolRepository = cardPoolRepository;
    }

    @GetMapping("/api/card-pools")
    public List<CardPoolResponse> listCardPools() {
        syncCardPoolsFromFiles();
        return cardPoolRepository.findAll().stream()
                .map(CardPoolResponse::from)
                .toList();
    }

    private void syncCardPoolsFromFiles() {
        listPoolFiles().forEach(filename -> {
            Matcher matcher = POOL_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                return;
            }
            String index = matcher.group(1);
            String poolKey = "pool" + index;
            String imagePath = "/images/cardPool/" + filename;
            CardPool pool = cardPoolRepository.findByPoolKey(poolKey)
                    .orElseGet(() -> {
                        CardPool created = new CardPool();
                        created.setPoolKey(poolKey);
                        return created;
                    });
            boolean dirty = false;
            String expectedName = "卡池" + index;
            if (!expectedName.equals(pool.getName())) {
                pool.setName(expectedName);
                dirty = true;
            }
            if (!imagePath.equals(pool.getImage())) {
                pool.setImage(imagePath);
                dirty = true;
            }
            if (dirty) {
                cardPoolRepository.save(pool);
            }
        });
    }

    private List<String> listPoolFiles() {
        Path rootImages = Paths.get("..", "images", "cardPool").toAbsolutePath().normalize();
        Path frontedImages = Paths.get("..", "fronted", "images", "cardPool").toAbsolutePath().normalize();

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
                .filter(name -> POOL_PATTERN.matcher(name).matches())
                .distinct()
                .toList();
    }

    public record CardPoolResponse(String id, String poolKey, String name, String image) {
        public static CardPoolResponse from(CardPool pool) {
            return new CardPoolResponse(
                    String.valueOf(pool.getId()),
                    pool.getPoolKey(),
                    pool.getName(),
                    pool.getImage());
        }
    }
}
