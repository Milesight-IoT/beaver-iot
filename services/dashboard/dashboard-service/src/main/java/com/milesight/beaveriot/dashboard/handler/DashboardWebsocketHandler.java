package com.milesight.beaveriot.dashboard.handler;

import com.milesight.beaveriot.authentication.facade.IAuthenticationFacade;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.integration.model.event.WebSocketEvent;
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
import org.springframework.beans.factory.annotation.Autowired;
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
        String userId = authenticationFacade.getUserIdByToken(token);
        if (userId == null) {
            sendHttpResponse(ctx, request, HttpResponseStatus.FORBIDDEN);
            return;
        }
        WebSocketContext.addChannel(userId, ctx);
    }

    @Override
    public void handleTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        WebSocketEvent webSocketEvent = JsonUtils.fromJSON(msg.text(), WebSocketEvent.class);
        if (webSocketEvent == null || !WebSocketEvent.EventType.EXCHANGE.equals(webSocketEvent.getEventType())) {
            return;
        }
        DashboardExchangePayload payload = JsonUtils.fromJSON(JsonUtils.toJSON(webSocketEvent.getPayload()), DashboardExchangePayload.class);
        if (payload == null) {
            return;
        }
        String userId = WebSocketContext.getChannelByValue(ctx);
        log.info("userId:{}, handleTextMessage:{}", userId, webSocketEvent);
        DashboardWebSocketContext.addEntityKeys(userId, payload.getEntityKey());
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx) throws Exception {
        DashboardWebSocketContext.removeEntityKeys(WebSocketContext.getChannelByValue(ctx));
        WebSocketContext.removeChannelByValue(ctx);
    }

    @Override
    public void exception(ChannelHandlerContext ctx, Throwable cause) throws Exception {
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
