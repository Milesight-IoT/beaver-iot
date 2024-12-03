package com.milesight.beaveriot.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author loong
 * @date 2024/10/8 10:40
 */
@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final WebSocketProperties webSocketProperties;
    private final AbstractWebSocketHandler[] webSocketHandler;

    public WebSocketChannelInitializer(WebSocketProperties webSocketProperties, AbstractWebSocketHandler... webSocketHandler) {
        this.webSocketProperties = webSocketProperties;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(512 * 1024));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(webSocketHandler);
        pipeline.addLast(new WebSocketServerProtocolHandler(webSocketProperties.getContextPath()));
    }

}
