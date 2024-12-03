package com.milesight.beaveriot.websocket;

import io.netty.channel.EventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author loong
 * @date 2024/10/21 8:56
 */
@Component
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnClass(EventLoopGroup.class)
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
@Slf4j
public class WebSocketNettyRunner implements CommandLineRunner {

    private final WebSocketProperties webSocketProperties;
    private final WebSocketChannelInitializer webSocketChannelInitializer;

    public WebSocketNettyRunner(WebSocketProperties webSocketProperties, WebSocketChannelInitializer webSocketChannelInitializer) {
        this.webSocketProperties = webSocketProperties;
        this.webSocketChannelInitializer = webSocketChannelInitializer;
    }

    @Override
    public void run(String... args) throws Exception {
        ThreadPoolExecutor executor = createThreadPoolExecutor();
        executor.execute(() -> {
            try {
                new WebSocketNettyServer().start(webSocketProperties.getPort(), webSocketChannelInitializer);
            } catch (Exception e) {
                log.error("init websocket error:{}", e.getMessage(), e);
            } finally {
                executor.shutdown();
            }
        });
    }

    private ThreadPoolExecutor createThreadPoolExecutor() {
        int corePoolSize = 1;
        int maxPoolSize = 1;
        long keepAliveTime = 0L;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        return new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue,
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }
}
