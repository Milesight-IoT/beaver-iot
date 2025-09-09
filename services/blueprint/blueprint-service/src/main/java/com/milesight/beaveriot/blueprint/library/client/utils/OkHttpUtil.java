package com.milesight.beaveriot.blueprint.library.client.utils;

import com.milesight.beaveriot.blueprint.library.client.response.ClientResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtil {

    @Getter
    private static final OkHttpClient client;

    private static final Map<String, String> commonHeaders = new ConcurrentHashMap<>();

    static {
        // Initialize OkHttpClient and set timeout
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)  // Connection timeout
                .readTimeout(120, TimeUnit.SECONDS)    // Read timeout
                .writeTimeout(10, TimeUnit.SECONDS)    // Write timeout
                .build();
    }

    public static void updateCommonHeaders(Map<String, String> commonHeaders) {
        OkHttpUtil.commonHeaders.putAll(commonHeaders);
    }

    public static ClientResponse get(String url) {
        return get(url, null);
    }

    /**
     * Sends a GET request
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @return The response result
     */
    public static ClientResponse get(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        return request(builder, headers);
    }

    /**
     * Sends a POST request (JSON data)
     *
     * @param url     The request URL
     * @param json    The request body in JSON format
     * @return The response result
     */
    public static ClientResponse post(String url, String json) {
        return post(url, null, json);
    }

    /**
     * Sends a POST request (JSON data)
     *
     * @param url     The request URL
     * @param headers The request headers (can be null)
     * @param json    The request body in JSON format
     * @return The response result
     */
    public static ClientResponse post(String url, Map<String, String> headers, String json) {
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        return request(builder, headers);
    }

    public static ClientResponse postForm(String url, Map<String, String> formData) {
        return postForm(url, null, formData);
    }

    public static ClientResponse postForm(String url, Map<String, String> headers, Map<String, String> formData) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }
        RequestBody body = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).post(body);
        return request(builder, headers);
    }

    private static ClientResponse request(Request.Builder builder, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        if (!commonHeaders.isEmpty()) {
            headers.putAll(commonHeaders);
        }
        addHeaders(builder, headers);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            return ClientResponse.builder()
                    .isSuccessful(response.isSuccessful())
                    .code(response.code())
                    .message(response.message())
                    .data(response.body() == null ? null :response.body().string())
                    .build();
        } catch (IOException e) {
            log.error("Failed to execute request: " + e.getMessage());
            return null;
        }
    }

    /**
     * Adds request headers
     *
     * @param builder The request builder
     * @param headers The request headers
     */
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }
}
