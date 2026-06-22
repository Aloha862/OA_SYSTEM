package com.example.oa.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final FileProperties fileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = fileProperties.getAccessPrefix();
        String path = Path.of(fileProperties.getUploadPath()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(path.endsWith("/") ? path : path + "/");
    }
}
