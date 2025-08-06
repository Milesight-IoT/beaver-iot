package com.milesight.beaveriot.context.i18n.support;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.i18n.config.MessageConfig;
import com.milesight.beaveriot.context.i18n.message.MessageResolver;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * author: Luxb
 * create: 2025/8/1 9:12
 **/
@SuppressWarnings("unused")
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
     * <p>For example, if {@code moduleName = "example-module"}, the system will load:
     * <pre>
     * i18n/example-module/messages_en.properties
     * i18n/example-module/messages_zh_CN.properties
     * i18n/example-module/messages_ja.properties
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
        return getResolver(null, moduleName);
    }

    /**
     * Returns a message resolver for a specific integration module.
     *
     * <p>This resolver loads message files from the integration-specific directory structure:
     * <pre>
     * i18n/integrations/{integrationId}/messages_en.properties
     * i18n/integrations/{integrationId}/messages_zh_CN.properties
     * i18n/integrations/{integrationId}/messages_ja.properties
     * </pre>
     * If a moduleName is provided, it further loads from:
     * <pre>
     * i18n/integrations/{integrationId}/{moduleName}/messages_en.properties
     * i18n/integrations/{integrationId}/{moduleName}/messages_zh_CN.properties
     * i18n/integrations/{integrationId}/{moduleName}/messages_ja.properties
     * </pre>
     *
     * <p>For example, if {@code integrationId = "example-integration"} and {@code moduleName = "example-module"},
     * the system will load:
     * <pre>
     * i18n/integrations/example-integration/example-module/messages_en.properties
     * i18n/integrations/example-integration/example-module/messages_zh_CN.properties
     * i18n/integrations/example-integration/example-module/messages_ja.properties
     * </pre>
     * If {@code integrationId = "mqtt-device"} and {@code moduleName = null}, the system will load:
     * <pre>
     * i18n/integrations/example-integration/messages_en.properties
     * i18n/integrations/example-integration/messages_zh_CN.properties
     * i18n/integrations/example-integration/messages_ja.properties
     * </pre>
     *
     * <p>It corresponds to the Spring configuration:
     * <pre>
     * spring:
     *   messages:
     *     basename: i18n/messages
     * </pre>
     *
     * @param integrationId the identifier of the integration
     * @param moduleName the module name used to locate the message file directory;
     *                   if null or empty, the resolver loads messages from the integration root directory
     * @return the message resolver for the specified integration and module (singleton)
     */
    public MessageResolver getResolver(String integrationId, String moduleName) {
        String beanName = getMessageResolverBeanName(integrationId, moduleName);
        if (applicationContext.containsBean(beanName)) {
            return applicationContext.getBean(beanName, MessageResolver.class);
        }

        MessageResolver resolver = new MessageResolver(config, integrationId, moduleName);
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
        return getDefaultResolver(null);
    }

    /**
     * Returns the default message resolver for a specific integration.
     *
     * <p>This resolver loads message files from the integration's root directory:
     * <pre>
     * i18n/integrations/{integrationId}/messages_en.properties
     * i18n/integrations/{integrationId}/messages_zh_CN.properties
     * i18n/integrations/{integrationId}/messages_ja.properties
     * </pre>
     *
     * <p>For example, if {@code integrationId = "example-integration"}, the system will load:
     * <pre>
     * i18n/integrations/example-integration/messages_en.properties
     * i18n/integrations/example-integration/messages_zh_CN.properties
     * i18n/integrations/example-integration/messages_ja.properties
     * </pre>
     *
     * <p>It corresponds to the Spring configuration:
     * <pre>
     * spring:
     *   messages:
     *     basename: i18n/messages
     * </pre>
     *
     * @param integrationId the identifier of the integration;
     *                      if null or empty, the global default resolver is used
     * @return the default message resolver for the specified integration (singleton)
     */
    public MessageResolver getDefaultResolver(String integrationId) {
        return getResolver(integrationId, null);
    }

    private String getMessageResolverBeanName(String integrationId, String moduleName) {
        return (StringUtils.isEmpty(integrationId) && StringUtils.isEmpty(moduleName))
                ? "defaultMessageResolver"
                : MessageResolver.class.getSimpleName() + ":" +
                Optional.ofNullable(integrationId).orElse("") + ":" +
                Optional.ofNullable(moduleName).orElse("");
    }
}