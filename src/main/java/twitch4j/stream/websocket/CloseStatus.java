package twitch4j.stream.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

/**
 * Container for WebSocket "close" status codes and reasons.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1"> RFC 6455, Section 7.4.1 "Defined Status Codes"</a>
 */
@Getter
@RequiredArgsConstructor
public class CloseStatus {

    private final int code;
    @Nullable
    private final String reason;

    @Override
    public String toString() {
        return code + (reason == null || reason.isEmpty() ? "" : " " + reason);
    }
}
