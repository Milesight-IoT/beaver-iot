package com.milesight.beaveriot.context.i18n;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.i18n.message.MergedResourceBundleMessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;

import java.util.Locale;

/**
 * author: Luxb
 * create: 2025/8/11 13:26
 **/
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MergedResourceBundleMessageSourceTests.TestConfig.class)
public class MergedResourceBundleMessageSourceTests {
    @Autowired
    private MergedResourceBundleMessageSource messageSource;

    @Test
    public void testGetSimpleMessage() {
        System.out.println("testGetSimpleMessage");
        String message = messageSource.getMessage("application-standard-test.hello.message");
        System.out.println("simple message: " + message);
        Assert.isTrue(!StringUtils.isEmpty(message), "message must not be empty");
    }

    @Test
    public void testGetMessageWithArgs() {
        System.out.println("testGetMessageWithArgs");
        String message = messageSource.getMessage("application-standard-test.hello.message.with.args", new Object[]{"luxb", "Milesight"});
        System.out.println("with args message: " + message);
        Assert.isTrue(!StringUtils.isEmpty(message), "message must not be empty");
    }

    @Test
    public void testGetChineseMessage() {
        System.out.println("testGetChineseMessage");
        String message = messageSource.getMessage("application-standard-test.hello.message.with.args", new Object[]{"luxb", "Milesight"}, Locale.SIMPLIFIED_CHINESE);
        System.out.println("chinese message: " + message);
        Assert.isTrue(!StringUtils.isEmpty(message), "message must not be empty");
    }

    @Test
    public void testGetMessageWithDefaultMessage() {
        System.out.println("testGetMessageWithDefaultMessage");
        String message = messageSource.getMessage("application-standard-test.message.not.found", null, "This is a default message");
        System.out.println("default message: " + message);
        Assert.isTrue(!StringUtils.isEmpty(message), "message must not be empty");
    }

    @Configuration
    static class TestConfig {
        @Bean
        public MergedResourceBundleMessageSource messageSource(PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver) {
            MergedResourceBundleMessageSource messageSource = new MergedResourceBundleMessageSource(pathMatchingResourcePatternResolver);
            messageSource.setBasename("i18n/messages");
            messageSource.setDefaultLocale(Locale.ROOT);
            messageSource.setDefaultEncoding("UTF-8");
            return messageSource;
        }

        @Bean
        public PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver() {
            return new PathMatchingResourcePatternResolver();
        }
    }
}