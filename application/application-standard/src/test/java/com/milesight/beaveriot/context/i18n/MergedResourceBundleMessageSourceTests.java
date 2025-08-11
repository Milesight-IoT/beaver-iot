package com.milesight.beaveriot.context.i18n;

import com.milesight.beaveriot.context.i18n.message.MergedResourceBundleMessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wildfly.common.Assert;

import java.util.Locale;

/**
 * author: Luxb
 * create: 2025/8/11 13:26
 **/
@SpringBootTest
public class MergedResourceBundleMessageSourceTests {
    @Autowired
    private MergedResourceBundleMessageSource messageSource;

    @Test
    public void testGetSimpleMessage() {
        System.out.println("testGetSimpleMessage");
        String message = messageSource.getMessage("application-standard-test.hello.message");
        System.out.println("simple message: " + message);
        Assert.assertNotNull(message);
    }

    @Test
    public void testGetMessageWithArgs() {
        System.out.println("testGetMessageWithArgs");
        String message = messageSource.getMessage("application-standard-test.hello.message.with.args", new Object[]{"luxb", "Milesight"});
        System.out.println("with args message: " + message);
        Assert.assertNotNull(message);
    }

    @Test
    public void testGetChineseMessage() {
        System.out.println("testGetChineseMessage");
        String message = messageSource.getMessage("application-standard-test.hello.message.with.args", new Object[]{"luxb", "Milesight"}, Locale.SIMPLIFIED_CHINESE);
        System.out.println("chinese message: " + message);
        Assert.assertNotNull(message);
    }

    @Test
    public void testGetMessageWithDefaultMessage() {
        System.out.println("testGetMessageWithDefaultMessage");
        String message = messageSource.getMessage("application-standard-test.message.not.found", null, "This is a default message");
        System.out.println("default message: " + message);
        Assert.assertNotNull(message);
    }
}