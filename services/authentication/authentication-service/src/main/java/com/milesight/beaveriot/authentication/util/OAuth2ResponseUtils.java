package com.milesight.beaveriot.authentication.util;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.utils.JsonUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author loong
 * @date 2024/10/16 8:59
 */
public class OAuth2ResponseUtils {

    public static void response(HttpServletResponse response, int httpStatus, ResponseBody responseBody) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.getWriter().write(JsonUtils.toJSON(responseBody));
    }

}
