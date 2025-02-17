package com.milesight.beaveriot.context.security;

import org.springframework.util.ObjectUtils;

/**
 * @author leon
 */
public class SecurityUserContext {

    public static final String USER_ID = "userId";
    public static final String NICK_NAME = "nickname";
    public static final String EMAIL = "email";
    public static final String CREATE_AT = "createdAt";
    private static final ThreadLocal<SecurityUser> securityUserThreadLocal = new ThreadLocal<>();

    public static SecurityUser getSecurityUser() {
        return securityUserThreadLocal.get();
    }

    public static void setSecurityUser(SecurityUser securityUser) {
        securityUserThreadLocal.set(securityUser);
        TenantContext.setTenantId(securityUser.getTenantId());
    }

    public static void clear() {
        securityUserThreadLocal.remove();
        TenantContext.clear();
    }

    public static Long getUserId() {
        SecurityUser securityUser = getSecurityUser();
        if (securityUser == null) {
            return null;
        }
        if (ObjectUtils.isEmpty(securityUser.getUserId())) {
            return null;
        }
        return securityUser.getUserId();
    }

}
