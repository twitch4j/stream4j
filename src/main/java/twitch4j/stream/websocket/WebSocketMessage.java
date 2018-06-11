package twitch4j.stream.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

/**
 * A text or binary message received on a {@link WebSocketSession}.
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public class WebSocketMessage {

    @NonNull
    private final Type type;
    @NonNull
    private final ByteBuf payload;

    /**
     * Create a new WebSocket message from a string.
     *
     * @param payload the payload contents
     * @return a {@code WebSocketMessage} with text contents
     */
    public static WebSocketMessage fromText(String payload) {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        return new WebSocketMessage(WebSocketMessage.Type.TEXT, buffer);
    }

    /**
     * Create a new text WebSocket message from a byte buffer.
     *
     * @param payload the payload contents
     * @return a {@code WebSocketMessage} with text contents
     */
    public static WebSocketMessage fromText(ByteBuf payload) {
        return new WebSocketMessage(WebSocketMessage.Type.TEXT, payload);
    }

    /**
     * Create a new closing WebSocket message.
     *
     * @return a {@code WebSocketMessage} with close type
     */
    public static WebSocketMessage close() {
        return new WebSocketMessage(WebSocketMessage.Type.CLOSE, Unpooled.buffer(0));
    }

    /**
     * Create a new binary WebSocket message from a byte buffer.
     *
     * @param payload the payload contents
     * @return a {@code WebSocketMessage} with binary contents
     */
    public static WebSocketMessage fromBinary(ByteBuf payload) {
        return new WebSocketMessage(Type.BINARY, payload);
    }

    /**
     * Create a new WebSocket message from a WebSocket frame.
     *
     * @param frame the original frame
     * @return the message built from the given frame
     */
    public static WebSocketMessage fromFrame(WebSocketFrame frame) {
        ByteBuf payload = frame.content();
        return new WebSocketMessage(Type.fromFrameClass(frame.getClass()), payload);
    }

    /**
     * Create a new WebSocket frame from a WebSocket message.
     *
     * @param message the original message
     * @return the frame built from the given message
     */
    public static WebSocketFrame toFrame(WebSocketMessage message) {
        ByteBuf byteBuf = message.getPayload();

        switch (message.getType()) {
            case TEXT:
                return new TextWebSocketFrame(byteBuf);
            case BINARY:
                return new BinaryWebSocketFrame(byteBuf);
            case CLOSE:
                return new CloseWebSocketFrame(1000, "Logging off");
            default:
                throw new IllegalArgumentException("Unknown websocket message type: " + message.getType());
        }
    }

    /**
     * Return the message type (text, binary, etc).
     *
     * @return the message type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Return the message payload.
     *
     * @return the payload represented as a byte buffer
     */
    public ByteBuf getPayload() {
        return this.payload;
    }

    /**
     * Return the message payload as UTF-8 text. This is a useful for text WebSocket messages.
     *
     * @return the payload represented as a String
     */
    public String getPayloadAsText() {
        byte[] bytes = new byte[this.payload.readableBytes()];
        this.payload.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * WebSocket message types.
     */
    public enum Type {
        TEXT, BINARY, CLOSE;

        public static Type fromFrameClass(Class<?> clazz) {
            if (clazz.equals(TextWebSocketFrame.class)) {
                return TEXT;
            } else if (clazz.equals(BinaryWebSocketFrame.class)) {
                return BINARY;
            } else if (clazz.equals(CloseWebSocketFrame.class)) {
                return CLOSE;
            }

            throw new IllegalArgumentException("Unknown frame class: " + clazz);
        }
    }

}
