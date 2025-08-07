package com.milesight.beaveriot.context.configuration;

import com.milesight.beaveriot.context.i18n.interceptor.LocaleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * author: Luxb
 * create: 2025/8/1 11:27
 **/
@Configuration
public class ContextWebMvcConfigurer implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LocaleInterceptor()).addPathPatterns("/**");
    }
}