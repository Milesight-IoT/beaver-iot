package com.milesight.beaveriot.rule.components.httpin;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.rule.components.httpin.model.HttpInRequestContent;
import com.milesight.beaveriot.rule.components.httpin.model.ListenConfig;
import com.milesight.beaveriot.rule.support.JsonHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * HttpInUrlManager class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Slf4j
@RestController
public class HttpInRequestListener {

    @Autowired
    private CredentialsServiceProvider credentialsServiceProvider;

    /**
     * TenantId -> FlowId -> ListenConfig
     */
    Map<String, Map<String, ListenConfig>> listeners = new ConcurrentHashMap<>();

    @RequestMapping(HttpInConstants.URL_PREFIX + "/{credentialName}@{tenantId}/**")
    public ResponseBody<Void> requestHttpIn(
            @PathVariable("credentialName") String credentialName,
            @PathVariable("tenantId") String tenantId,
            @RequestBody(required = false) byte[] body,
            @RequestParam(required = false) Map<String, String> params,
            HttpServletRequest httpRequest
    ) {
        String path = httpRequest.getServletPath();
        String[] pathParts = path.split("/", 4);
        if (pathParts.length != 4 || !StringUtils.hasText(pathParts[3])) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR.getErrorCode(), "invalid request path").build();
        }

        if (httpRequest.getContentLengthLong() > HttpInConstants.MAX_REQUEST_SIZE) {
            throw ServiceException.with(ErrorCode.DATA_TOO_LARGE.getErrorCode(), "data too large").build();
        }

        String customPath = pathParts[3];
        Map<String, ListenConfig> listenConfigMap = listeners.get(tenantId);
        if (listenConfigMap == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR.getErrorCode(), "path not configured").build();
        }

        TenantContext.setTenantId(tenantId);
        // [Auth] Authentication
        Long credentialsId = checkAuth(httpRequest.getHeader(HttpInConstants.AUTH_HEADER));
        if (credentialsId == null) {
            throw ServiceException.with(ErrorCode.AUTHENTICATION_FAILED.getErrorCode(), "auth failed").build();
        }

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = httpRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = httpRequest.getHeader(headerName);
            headers.put(headerName, headerValue);
        }

        listenConfigMap
                .values().stream()
                .filter(c -> c.getMethod().equals(httpRequest.getMethod())
                        && Objects.equals(c.getCredentialsId(), credentialsId) // [Auth] Authorization
                        && c.getUrlTemplate().matches(customPath)
                )
                .forEach(c -> {
                    HttpInRequestContent content = new HttpInRequestContent();
                    content.setUrl(customPath);
                    content.setMethod(httpRequest.getMethod());
                    content.setHeaders(headers);
                    content.setPathParams(c.getUrlTemplate().match(customPath));
                    content.setParams(params);

                    try {
                        content.setBody(parseBody(httpRequest, body, params));
                    } catch (Exception e) {
                        log.error("Parse body error: " + e.getMessage());
                    }

                    log.debug("Request http in: {}", content);
                    c.getCb().accept(content);
                });
        return ResponseBuilder.success();
    }

    public void registerUrl(String tenantId, String flowId, ListenConfig config) {
        listeners.computeIfAbsent(tenantId, tid -> new ConcurrentHashMap<>()).put(flowId, config);
    }

    public void unregisterUrl(String tenantId, String flowId) {
        Map<String, ListenConfig> listenConfigMap = listeners.get(tenantId);
        if (listenConfigMap == null) {
            return;
        }

        listenConfigMap.remove(flowId);
    }

    private Long checkAuth(String auth) {
        if (!auth.startsWith(HttpInConstants.BASIC_AUTH_PREFIX)) {
            return null;
        }
        Credentials credentials = credentialsServiceProvider.getOrCreateCredentials(CredentialsType.HTTP);
        String inputAuth = new String(Base64.getDecoder().decode(auth.substring(HttpInConstants.BASIC_AUTH_PREFIX.length())), StandardCharsets.UTF_8);
        String savedAuth = credentials.getAccessKey() + ":" + credentials.getAccessSecret();
        return inputAuth.equals(savedAuth) ? credentials.getId() : null;
    }

    private String parseBody(HttpServletRequest httpRequest, byte[] body, Map<String, String> params) throws ServletException, IOException {
        // resolve content type -> form-data
        MediaType contentType = httpRequest.getContentType() != null ? MediaType.parseMediaType(httpRequest.getContentType()) : MediaType.APPLICATION_OCTET_STREAM;
        if (contentType.getType().equals(MediaType.MULTIPART_FORM_DATA.getType())) {
            httpRequest.getParts().forEach(part -> {
                try {
                    if (part.getSubmittedFileName() != null) {
                        params.put(part.getName(), Base64.getEncoder().encodeToString(part.getInputStream().readAllBytes()));
                    }
                } catch (Exception e) {
                    log.error("Read file {} error: {}", part.getName(), e.getMessage());
                }
            });

            return null;
        }

        byte[] bytesToEncode = (body != null && body.length > 0) ? body : httpRequest.getInputStream().readAllBytes();

        if (bytesToEncode.length > 0) {
            return new String(bytesToEncode, httpRequest.getCharacterEncoding());
        }

        return null;
    }
}
