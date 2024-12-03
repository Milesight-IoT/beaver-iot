package com.milesight.beaveriot.authentication.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author loong
 * @date 2024/10/12 9:18
 */
public class OAuth2EndpointUtils {
    public static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                String[] var3 = values;
                int var4 = values.length;
                for (int var5 = 0; var5 < var4; ++var5) {
                    String value = var3[var5];
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }

    public static OrRequestMatcher getWhiteListMatcher(String[] whiteList) {
        return new OrRequestMatcher(
                Stream.of(whiteList)
                        .map(AntPathRequestMatcher::new)
                        .collect(Collectors.toList())
        );
    }


    public static void throwError(String errorCode, String description, String errorUri) {
        OAuth2Error error = new OAuth2Error(errorCode, description, errorUri);
        throw new OAuth2AuthenticationException(error);
    }
}
