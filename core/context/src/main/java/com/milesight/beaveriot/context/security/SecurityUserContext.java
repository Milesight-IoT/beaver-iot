package com.milesight.beaveriot.context.security;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * @author leon
 */
public class SecurityUserContext {

    public static final String USER_ID = "USER_ID";
    public static final String TENANT_ID = "TENANT_ID";

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

    public static Long getUserId() {
        SecurityUser securityUser = getSecurityUser();
        if (securityUser == null) {
            return null;
        }
        if (securityUser.getPayload().get(USER_ID) == null) {
            return null;
        }
        return Long.parseLong(securityUser.getPayload().get(USER_ID).toString());
    }

    public static Long getTenantId() {
        SecurityUser securityUser = getSecurityUser();
        if (securityUser == null) {
            return null;
        }
        if (securityUser.getPayload().get(TENANT_ID) == null) {
            return null;
        }
        return Long.parseLong(securityUser.getPayload().get(TENANT_ID).toString());
    }

}
