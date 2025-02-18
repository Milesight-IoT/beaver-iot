package com.milesight.beaveriot.dashboard.handler;

import com.milesight.beaveriot.authentication.facade.IAuthenticationFacade;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.model.event.WebSocketEvent;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.dashboard.context.DashboardWebSocketContext;
import com.milesight.beaveriot.dashboard.model.DashboardExchangePayload;
import com.milesight.beaveriot.websocket.AbstractWebSocketHandler;
import com.milesight.beaveriot.websocket.WebSocketContext;
import com.milesight.beaveriot.websocket.WebSocketProperties;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/10/18 11:19
 */
@Component
@Getter
@Slf4j
public class DashboardWebsocketHandler extends AbstractWebSocketHandler {

    private final ObjectProvider<IAuthenticationFacade> authenticationFacadeObjectProvider;

    public DashboardWebsocketHandler(WebSocketProperties webSocketProperties, ObjectProvider<IAuthenticationFacade> authenticationFacade) {
        super(webSocketProperties);
        this.authenticationFacadeObjectProvider = authenticationFacade;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, List<String>> urlParams) throws Exception {
        String token = getToken(request, urlParams);
        if (token == null) {
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            return;
        }
        IAuthenticationFacade authenticationFacade = authenticationFacadeObjectProvider.getIfAvailable();
        if (authenticationFacade == null) {
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            return;
        }
        Map<String, Object> user = authenticationFacade.getUserByToken(token);
        if (user == null || user.isEmpty()) {
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            return;
        }
        String tenantId = user.get(SecurityUserContext.TENANT_ID).toString();
        String userId = user.get(SecurityUserContext.USER_ID).toString();
        String key = tenantId + WebSocketContext.KEY_JOIN_SYMBOL + userId;
        WebSocketContext.addChannel(key, ctx);
        log.debug("connect:key:{}, channelId:{}", key, ctx.channel().id());
    }

    @Override
    public void handleTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        WebSocketEvent webSocketEvent = JsonUtils.fromJSON(msg.text(), WebSocketEvent.class);
        if (webSocketEvent == null) {
            return;
        }
        if (WebSocketEvent.EventType.HEARTBEAT.equalsIgnoreCase(webSocketEvent.getEventType())) {
            TextWebSocketFrame retainedMsg = msg.retain();
            ctx.channel().writeAndFlush(retainedMsg);
            return;
        }
        if (!WebSocketEvent.EventType.EXCHANGE.equalsIgnoreCase(webSocketEvent.getEventType())) {
            return;
        }
        DashboardExchangePayload payload = JsonUtils.fromJSON(JsonUtils.toJSON(webSocketEvent.getPayload()), DashboardExchangePayload.class);
        if (payload == null) {
            return;
        }
        String key = WebSocketContext.getKeyByValue(ctx);
        log.info("key:{}, handleTextMessage:{}", key, webSocketEvent);
        DashboardWebSocketContext.addEntityKeys(key, payload.getEntityKey());
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx) throws Exception {
        log.debug("disconnect:key:{}, channelId:{}", WebSocketContext.getKeyByValue(ctx), ctx.channel().id());
        DashboardWebSocketContext.removeEntityKeys(WebSocketContext.getKeyByValue(ctx));
        WebSocketContext.removeChannelByValue(ctx);
    }

    @Override
    public void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exception:key:{}, channelId:{}", WebSocketContext.getKeyByValue(ctx), ctx.channel().id(), cause);
    }

    private String getToken(FullHttpRequest request, Map<String, List<String>> urlParams) {
        String authorizationValue = request.headers().get("Authorization");
        if (!StringUtils.hasText(authorizationValue)) {
            authorizationValue = urlParams.get("Authorization") == null ? null : urlParams.get("Authorization").get(0);
        }
        if (authorizationValue != null && authorizationValue.startsWith("Bearer ")) {
            return authorizationValue.substring(7);
        }
        return null;
    }

}
