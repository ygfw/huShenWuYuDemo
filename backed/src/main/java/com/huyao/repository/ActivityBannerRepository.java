package com.huyao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.huyao.entity.ActivityBanner;

public interface ActivityBannerRepository extends JpaRepository<ActivityBanner, Long> {
    Optional<ActivityBanner> findByBannerKey(String bannerKey);
}
