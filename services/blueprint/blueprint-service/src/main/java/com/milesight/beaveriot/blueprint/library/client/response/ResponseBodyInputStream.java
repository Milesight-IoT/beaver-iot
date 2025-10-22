package com.milesight.beaveriot.blueprint.library.client.response;

import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;

/**
 * author: Luxb
 * create: 2025/10/22 14:38
 **/
public class ResponseBodyInputStream extends InputStream {
    private final InputStream inputStream;
    private final Response response;

    public ResponseBodyInputStream(Response response) {
        this.response = response;
        this.inputStream = response.body() != null ? response.body().byteStream() : null;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public void close() throws IOException {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}