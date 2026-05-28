package com.englishplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Map /uploads/** to the uploads/ directory on disk.
     * Spring's static-locations alone doesn't map /uploads/... → uploads/...
     * (it would look for uploads/uploads/... which doesn't exist).
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.startsWith("/")
                ? "file:" + uploadDir
                : "file:./" + uploadDir;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
