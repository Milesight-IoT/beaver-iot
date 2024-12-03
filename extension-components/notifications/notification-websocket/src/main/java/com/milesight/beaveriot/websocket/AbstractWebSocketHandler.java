package com.milesight.beaveriot.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 */
@ChannelHandler.Sharable
public abstract class AbstractWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final WebSocketProperties webSocketProperties;

    public AbstractWebSocketHandler(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }

    /**
     * When establishing a connection, you can implement this method to perform some initialization operations. The method being called does not necessarily mean that the handshake will succeed
     */
    public abstract void connect(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, List<String>> urlParams) throws Exception;

    /**
     * You can process the received websocket messages
     */
    public abstract void handleTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception;

    /**
     * When the connection is disconnected, this method will be called, and you can implement this method to release resources
     */
    public abstract void disconnect(ChannelHandlerContext ctx) throws Exception;

    public abstract void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception;

    @Override
    public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
        disconnect(ctx);
        if (!ctx.channel().isActive()) {
            return;
        }
        super.channelInactive(ctx);
    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            if (!request.decoderResult().isSuccess() ||
                    (!"websocket".equals(request.headers().get("Upgrade")))) {
                sendHttpResponse(ctx, request, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            String path = request.uri();
            if (path.contains("?")) {
                path = path.substring(0, path.indexOf("?"));
            }
            if (!path.equals(webSocketProperties.getContextPath())) {
                sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
                return;
            }
            String uri = request.uri();
            Map<String, List<String>> urlParams = getUrlParams(uri);
            if (uri.contains("?")) {
                String newUri = uri.substring(0, uri.indexOf("?"));
                request.setUri(newUri);
            }
            connect(ctx, request, urlParams);
        } else if (msg instanceof TextWebSocketFrame textWebSocketFrame) {
            handleTextMessage(ctx, textWebSocketFrame);
        }
        if (!ctx.channel().isActive()) {
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exception(ctx, cause);
        if (!ctx.channel().isActive()) {
            return;
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    }

    private static Map<String, List<String>> getUrlParams(String urlString) throws Exception {
        Map<String, List<String>> params = new HashMap<>();
        String pathAndQuery = urlString;
        int queryIndex = pathAndQuery.indexOf('?');
        if (queryIndex != -1) {
            String query = pathAndQuery.substring(queryIndex + 1);

            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);

                if (!params.containsKey(key)) {
                    params.put(key, new ArrayList<>());
                }
                params.get(key).add(value);
            }
        }
        return params;
    }

    public void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, HttpResponseStatus responseStatus) {
        FullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus);
        if (res.status().code() != HttpResponseStatus.OK.code()) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != HttpResponseStatus.OK.code()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

}
