package com.milesight.beaveriot.context.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * @author leon
 */
public class SecurityUserContext {

    public static final String USER_ID = "USER_ID";

    private static final ThreadLocal<SecurityUser> securityUserThreadLocal = new ThreadLocal<>();

    public static SecurityUser getSecurityUser() {
        return securityUserThreadLocal.get();
    }

    public static void setSecurityUser(SecurityUser securityUser) {
        securityUserThreadLocal.set(securityUser);
    }

    public static void clear() {
        securityUserThreadLocal.remove();
    }

    @Builder
    @Getter
    public static class SecurityUser {
        private Map<String, Object> header;
        private Map<String, Object> payload;
    }

    public static String getUserId() {
        SecurityUser securityUser = getSecurityUser();
        if (securityUser == null) {
            return null;
        }
        if (securityUser.getPayload().get(USER_ID) == null) {
            return null;
        }
        return securityUser.getPayload().get(USER_ID).toString();
    }

}
