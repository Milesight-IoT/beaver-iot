package com.milesight.beaveriot.context.security;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
@SuperBuilder
@Getter
public class SecurityUser extends TenantId{

    private String email;

    private String nickname;

    private Long userId;

    private String createdAt;

    public static SecurityUser create(Map<String, Object> jwtClaims) {
        Assert.notNull(jwtClaims, "jwtClaims must not be null");
        return SecurityUser.builder()
                .tenantId(getClaimAsString(jwtClaims, TenantContext.TENANT_ID))
                .userId(getClaimAsLong(jwtClaims, SecurityUserContext.USER_ID))
                .nickname(getClaimAsString(jwtClaims, SecurityUserContext.NICK_NAME))
                .email(getClaimAsString(jwtClaims, SecurityUserContext.EMAIL))
                .createdAt(getClaimAsString(jwtClaims, SecurityUserContext.CREATE_AT))
                .build();
    }

    private static String getClaimAsString(Map<String, Object> jwtClaims, String nickName) {
        return jwtClaims.containsKey(nickName) ? jwtClaims.get(nickName).toString() : null;
    }

    private static Long getClaimAsLong(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return claims.containsKey(key) && !ObjectUtils.isEmpty(value) ? Long.valueOf(value.toString()) : null;
    }

}
