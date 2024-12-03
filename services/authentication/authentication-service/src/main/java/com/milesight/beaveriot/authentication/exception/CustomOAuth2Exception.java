package com.milesight.beaveriot.authentication.exception;

import com.milesight.beaveriot.authentication.util.OAuth2ResponseUtils;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

import java.io.IOException;

/**
 * @author loong
 * @date 2024/10/15 10:49
 */
@Getter
public class CustomOAuth2Exception {

    public static void exceptionResponse(HttpServletResponse response, Exception exception) throws IOException {
        String code = ErrorCode.AUTHENTICATION_FAILED.getErrorCode();
        int httpStatus = ErrorCode.AUTHENTICATION_FAILED.getStatus();
        String msg;
        if (exception instanceof OAuth2AuthenticationException e) {
            String errorCode = e.getError().getErrorCode();
            String description = e.getError().getDescription() == null ? "" : (":" + e.getError().getDescription());
            msg = errorCode + description;
        } else {
            msg = OAuth2ErrorCodes.INVALID_REQUEST + ":" + exception.getMessage();
        }
        ResponseBody responseBody = ResponseBuilder.fail(code, msg);
        OAuth2ResponseUtils.response(response, httpStatus, responseBody);
    }

}
