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

import com.huyao.entity.ActivityBanner;
import com.huyao.repository.ActivityBannerRepository;

@RestController
public class ActivityBannerController {
    private static final Pattern BANNER_PATTERN = Pattern
            .compile("^activity(\\d+)\\.(jpg|jpeg|png)$", Pattern.CASE_INSENSITIVE);

    private final ActivityBannerRepository activityBannerRepository;

    public ActivityBannerController(ActivityBannerRepository activityBannerRepository) {
        this.activityBannerRepository = activityBannerRepository;
    }

    @GetMapping("/api/activity-banners")
    public List<ActivityBannerResponse> listActivityBanners() {
        syncBannersFromFiles();
        return activityBannerRepository.findAll().stream()
                .map(ActivityBannerResponse::from)
                .toList();
    }

    private void syncBannersFromFiles() {
        listBannerFiles().forEach(filename -> {
            Matcher matcher = BANNER_PATTERN.matcher(filename);
            if (!matcher.matches()) {
                return;
            }
            String index = matcher.group(1);
            String bannerKey = "activity" + index;
            String imagePath = "/images/activities/" + filename;
            ActivityBanner banner = activityBannerRepository.findByBannerKey(bannerKey)
                    .orElseGet(() -> {
                        ActivityBanner created = new ActivityBanner();
                        created.setBannerKey(bannerKey);
                        return created;
                    });
            if (!imagePath.equals(banner.getImage())) {
                banner.setImage(imagePath);
                activityBannerRepository.save(banner);
            }
        });
    }

    private List<String> listBannerFiles() {
        Path rootImages = Paths.get("..", "images", "activities").toAbsolutePath().normalize();
        Path frontedImages = Paths.get("..", "fronted", "images", "activities").toAbsolutePath().normalize();

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
                .filter(name -> BANNER_PATTERN.matcher(name).matches())
                .distinct()
                .toList();
    }

    public record ActivityBannerResponse(String id, String bannerKey, String image) {
        public static ActivityBannerResponse from(ActivityBanner banner) {
            return new ActivityBannerResponse(
                    String.valueOf(banner.getId()),
                    banner.getBannerKey(),
                    banner.getImage());
        }
    }
}
