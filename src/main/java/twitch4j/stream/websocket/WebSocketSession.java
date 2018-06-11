package twitch4j.stream.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslCloseCompletionEvent;
import lombok.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.netty.NettyPipeline;
import reactor.ipc.netty.http.client.HttpClient;
import reactor.ipc.netty.http.websocket.WebsocketInbound;
import reactor.ipc.netty.http.websocket.WebsocketOutbound;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * WebSocket adapter around {@link reactor.ipc.netty.http.websocket.WebsocketInbound WebSocketInbound} and {@link
 * reactor.ipc.netty.http.websocket.WebsocketOutbound WebSocketOutbound}.
 */
@Getter
@ToString
public class WebSocketSession {

    private static final Logger log = Loggers.getLogger(WebSocketSession.class);

    @ToString.Exclude
    @Getter(AccessLevel.PRIVATE)
    private final WebSocketConnection delegate;

    /**
     * Return the id of the session.
     *
     * @return a {@code String} representing an hexadecimal number.
     */
    private final String id;

    public WebSocketSession(WebsocketInbound inbound, WebsocketOutbound outbound) {
        this.delegate = new WebSocketConnection(inbound, outbound);
        this.id = Integer.toHexString(System.identityHashCode(delegate));
    }

    /**
     * Get the flux of incoming messages, aggregated from frames.
     *
     * @return a {@code Flux<WebSocketMessage>} inbound from the connection.
     */
    public Flux<WebSocketMessage> receive() {
        return getDelegate().getInbound()
                .aggregateFrames()
                .receiveFrames()
                .map(WebSocketMessage::fromFrame);
    }

    /**
     * Write the given messages to the WebSocket connection.
     *
     * @param messages the messages to write
     * @return a Mono signaling completion
     */
    public Mono<Void> send(Publisher<WebSocketMessage> messages) {
        Flux<WebSocketFrame> frames = Flux.from(messages).map(WebSocketMessage::toFrame);
        return getDelegate().getOutbound()
                .options(NettyPipeline.SendOptions::flushOnEach)
                .sendObject(frames)
                .then();
    }

    /**
     * Replace internal reactor-netty logging handler for HttpClients with a custom one that provides more concise
     * information.
     */
    public void replaceLoggingHandler() {
        getDelegate().getInbound().context()
                .replaceHandler("reactor.left.loggingHandler",
                        new SimpleLoggingHandler(HttpClient.class, LogLevel.DEBUG));
    }

    /**
     * Get a future notifying the closing of the session.
     *
     * @return a Mono signaling completion, including the code and reason for the event.
     */
    public Mono<CloseStatus> closeFuture() {
        MonoProcessor<CloseStatus> reason = MonoProcessor.create();
        // listen to netty event loop to retrieve close reason
        getDelegate().getInbound().context().addHandlerLast("d4j.last.closeHandler",
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (msg instanceof CloseWebSocketFrame && ((CloseWebSocketFrame) msg).isFinalFragment()) {
                            CloseWebSocketFrame close = (CloseWebSocketFrame) msg;
                            log.debug("Close status detected: {} {}", close.statusCode(), close.reasonText());
                            // then push it to our MonoProcessor for the reason
                            reason.onNext(new CloseStatus(close.statusCode(), close.reasonText()));
                        }
                        ctx.fireChannelRead(msg);
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
                        if (evt instanceof SslCloseCompletionEvent) {
                            SslCloseCompletionEvent closeEvent = (SslCloseCompletionEvent) evt;
                            if (!closeEvent.isSuccess()) {
                                log.debug("Abnormal close status detected: {}", closeEvent.cause().toString());
                                // then push it to our MonoProcessor for the reason
                                if (!reason.isTerminated()) {
                                    reason.onError(closeEvent.cause());
                                }
                            }
                        }
                        ctx.fireUserEventTriggered(evt);
                    }
                });

        return reason;
    }

    /**
     * Simple container for {@link reactor.ipc.netty.http.websocket.WebsocketInbound WebSocketInbound} and {@link
     * reactor.ipc.netty.http.websocket.WebsocketOutbound WebSocketOutbound}.
     */
    @Getter
    @RequiredArgsConstructor
    private static class WebSocketConnection {
        private final WebsocketInbound inbound;
        private final WebsocketOutbound outbound;
    }

}
