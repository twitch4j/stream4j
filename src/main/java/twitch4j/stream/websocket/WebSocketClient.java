package twitch4j.stream.websocket;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.client.HttpClient;

/**
 * WebSocket client over Reactor Netty.
 */
@RequiredArgsConstructor
public class WebSocketClient {

    private final HttpClient httpClient;

    public WebSocketClient() {
        this(HttpClient.create());
    }

    /**
     * Execute a handshake request to the given url and handle the resulting WebSocket session with the given handler.
     *
     * @param url the handshake url
     * @param handler the handler of the WebSocket session
     * @return completion {@code Mono<Void>} to indicate the outcome of the WebSocket session handling.
     */
    public Mono<Void> execute(String url, WebSocketHandler handler) {
        return this.httpClient
                .ws(url, headers -> {}, null)
                .flatMap(response ->
                        response.receiveWebsocket((in, out) ->
                                handler.handle(new WebSocketSession(in, out))));
    }

}
