package com.milesight.beaveriot.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author loong
 * @date 2024/10/18 14:42
 */
public class WebSocketContext {

    private static final Map<String, List<ChannelHandlerContext>> channelMap = new ConcurrentHashMap<>();

    public static List<ChannelHandlerContext> getChannel(String key) {
        return channelMap.get(key);
    }

    public static List<ChannelHandlerContext> getChannelsByKeys(List<String> keys) {
        return channelMap.entrySet().stream().filter(entry -> keys.contains(entry.getKey())).map(Map.Entry::getValue).flatMap(List::stream).toList();
    }

    public static List<ChannelHandlerContext> getAllChannel() {
        return channelMap.values().stream().flatMap(List::stream).toList();
    }

    public static void removeAllChannel(String key) {
        channelMap.remove(key);
    }

    public static String getChannelByValue(ChannelHandlerContext ctx) {
        return channelMap.entrySet().stream().filter(entry -> entry.getValue().stream().anyMatch(ctx1 -> ctx1.channel().id().asLongText().equals(ctx.channel().id().asLongText()))).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static void removeChannelByValue(ChannelHandlerContext ctx) {
        for (String key : channelMap.keySet()) {
            List<ChannelHandlerContext> channelHandlerContexts = channelMap.get(key);
            channelHandlerContexts.removeIf(context -> context.channel().id().asLongText().equals(ctx.channel().id().asLongText()));
            if (channelHandlerContexts.isEmpty()) {
                channelMap.remove(key);
            }
        }
    }

    public static void addChannel(String key, ChannelHandlerContext ctx) {
        channelMap.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(ctx);
    }

    public static void sendMessage(String key, String message) {
        List<ChannelHandlerContext> channelHandlerContexts = channelMap.get(key);
        if (channelHandlerContexts != null && !channelHandlerContexts.isEmpty()) {
            channelHandlerContexts.forEach(ctx -> {
                if (ctx.channel().isActive()) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
                }
            });
        }
    }

    public static void sendMessage(List<String> keys, String message) {
        getChannelsByKeys(keys).forEach(ctx -> {
            if (ctx.channel().isActive()) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
            }
        });
    }

    public static void sendAllMessage(String message) {
        if (channelMap.isEmpty()) {
            return;
        }
        channelMap.values().stream().flatMap(List::stream).forEach(ctx -> {
            if (ctx.channel().isActive()) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
            }
        });
    }

}
