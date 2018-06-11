package twitch4j.stream.websocket;

import reactor.core.publisher.Mono;

/**
 * Handler for a WebSocket session.
 */
@FunctionalInterface
public interface WebSocketHandler {

    /**
     * Handle the WebSocket session.
     *
     * @param session the session to handle
     * @return completion {@code Mono<Void>} to indicate the outcome of the WebSocket session handling.
     */
    Mono<Void> handle(WebSocketSession session);

}
