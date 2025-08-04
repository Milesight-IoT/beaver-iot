package com.milesight.beaveriot.context.i18n.message;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/8/1 9:12
 **/
@Component
public class MessageResolverFactory {
    private final MessageConfig config;
    private final ConfigurableApplicationContext applicationContext;

    public MessageResolverFactory(MessageConfig config, ConfigurableApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
    }

    /**
     * Returns a message resolver for the specified module.
     *
     * <p>Message files should be organized under the classpath by module, following this structure:
     * <pre>
     * classpath:/i18n/{moduleName}/messages_*.properties
     * </pre>
     *
     * <p>For example, if {@code moduleName = "user"}, the system will load:
     * <pre>
     * i18n/user/messages_en.properties
     * i18n/user/messages_zh_CN.properties
     * i18n/user/messages_ja.properties
     * </pre>
     *
     * <p>It corresponds to the Spring configuration:
     * <pre>
     * spring:
     *   messages:
     *     basename: i18n/messages
     * </pre>
     *
     * @param moduleName the module name used to locate the message file directory;
     *                   if null or empty, the default message resolver is used
     * @return the message resolver for the given module (singleton)
     */
    public MessageResolver getResolver(String moduleName) {
        String beanName = getMessageResolverBeanName(moduleName);
        if (applicationContext.containsBean(beanName)) {
            return applicationContext.getBean(beanName, MessageResolver.class);
        }

        MessageResolver resolver = new MessageResolver(config, moduleName);
        applicationContext.getBeanFactory().registerSingleton(beanName, resolver);
        return resolver;
    }

    /**
     * Returns the default message resolver for global or shared messages.
     *
     * <p>This resolver loads message files from the root i18n directory:
     * <pre>
     * i18n/messages_en.properties
     * i18n/messages_zh_CN.properties
     * i18n/messages_ja.properties
     * </pre>
     *
     * <p>It corresponds to the Spring configuration:
     * <pre>
     * spring:
     *   messages:
     *     basename: i18n/messages
     * </pre>
     *
     * @return the default message resolver (singleton)
     */
    public MessageResolver getDefaultResolver() {
        return getResolver(null);
    }

    private String getMessageResolverBeanName(String moduleName) {
        return (moduleName == null || moduleName.isEmpty())
                ? "defaultMessageResolver"
                : MessageResolver.class.getSimpleName() + ":" + moduleName;
    }
}