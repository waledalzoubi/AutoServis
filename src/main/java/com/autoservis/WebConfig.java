package com.autoservis;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/giris", "/giris-yap", "/kayit", "/kayit-yap", "/musteri", "/musteri-sorgula",
                        "/musteri/takip/**", "/manifest.webmanifest", "/service-worker.js",
                        "/pwa-register.js", "/offline.html", "/brand/**", "/icons/**",
                        "/error", "/favicon.ico");
    }
}
