package com.milesight.beaveriot.context.i18n.support;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.context.i18n.annotation.AutowiredMessageResolver;
import com.milesight.beaveriot.context.i18n.message.MessageResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * author: Luxb
 * create: 2025/8/5 17:54
 **/
@Component
public class MessageResolverBeanSupporter {
    private final MessageResolverFactory messageResolverFactory;

    public MessageResolverBeanSupporter(MessageResolverFactory messageResolverFactory) {
        this.messageResolverFactory = messageResolverFactory;
    }

    public void injectMessageResolvers(Object bean, Class<?> clazz) {
        injectMessageResolvers(bean, clazz, null);
    }

    public void injectMessageResolvers(Object bean, Class<?> clazz, String integrationId) {
        ReflectionUtils.doWithFields(clazz, field -> {
            AutowiredMessageResolver ann = field.getAnnotation(AutowiredMessageResolver.class);
            if (ann != null) {
                try {
                    if (!MessageResolver.class.isAssignableFrom(field.getType())) {
                        throw new IllegalStateException(
                                "Field annotated with @AutowiredMessageResolver must be of type MessageResolver: " + field);
                    }

                    field.setAccessible(true);

                    String moduleName = ann.value();
                    MessageResolver resolver = StringUtils.isEmpty(moduleName)
                            ? messageResolverFactory.getDefaultResolver(integrationId)
                            : messageResolverFactory.getResolver(integrationId, moduleName);

                    field.set(bean, resolver);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to inject: " + field, e);
                }
            }
        });
    }
}