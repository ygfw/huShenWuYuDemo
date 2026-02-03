package com.huyao.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontedPath = Paths.get("..", "fronted").toAbsolutePath().normalize();
        String location = "file:" + frontedPath.toString() + "/";
        Path rootImages = Paths.get("..", "images").toAbsolutePath().normalize();
        String imagesLocation = "file:" + rootImages.toString() + "/";
        Path frontedImages = frontedPath.resolve("images").normalize();
        String frontedImagesLocation = "file:" + frontedImages.toString() + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesLocation, frontedImagesLocation)
                .setCachePeriod(0);
        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
    }
}
