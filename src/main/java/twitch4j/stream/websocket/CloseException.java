package twitch4j.stream.websocket;

/**
 * Unchecked exception thrown when a websocket session is closed, in an expected way or not.
 * <p>
 * Used to wrap an underlying websocket {@link CloseStatus} so clients can retrieve the
 * status code and perform actions after it.
 */
public class CloseException extends RuntimeException {

    private final CloseStatus closeStatus;

    public CloseException(CloseStatus closeStatus) {
        this(closeStatus, null);
    }

    public CloseException(CloseStatus closeStatus, Throwable cause) {
        super(cause);
        this.closeStatus = closeStatus;
    }

    public CloseStatus getCloseStatus() {
        return closeStatus;
    }

    public int getCode() {
        return closeStatus.getCode();
    }

    public String getReason() {
        return closeStatus.getReason();
    }

    @Override
    public String getMessage() {
        return "WebSocket closed: " + closeStatus.toString();
    }
}
