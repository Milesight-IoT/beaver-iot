package com.milesight.beaveriot.context.i18n.interceptor;

import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

/**
 * author: Luxb
 * create: 2025/8/1 8:43
 **/
public class LocaleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (localeResolver != null) {
            Locale locale = localeResolver.resolveLocale(request);
            LocaleContext.setLocale(locale);
        }
        return true;
    }
}
