package twitch4j.stream.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class SimpleLoggingHandler extends LoggingHandler {

    public SimpleLoggingHandler(Class<?> clazz, LogLevel level) {
        super(clazz, level);
    }

    @Override
    protected String format(ChannelHandlerContext ctx, String eventName, Object arg) {
        if (arg instanceof ByteBuf) {
            return formatByteBuf(ctx, eventName, (ByteBuf) arg);
        } else if (arg instanceof ByteBufHolder) {
            return formatByteBufHolder(ctx, eventName, (ByteBufHolder) arg);
        } else {
            return formatSimple(ctx, eventName, arg);
        }
    }

    /**
     * Generates the default log message of the specified event whose argument is a {@link io.netty.buffer.ByteBuf}.
     */
    private static String formatByteBuf(ChannelHandlerContext ctx, String eventName, ByteBuf msg) {
        String chStr = ctx.channel().toString();
        int length = msg.readableBytes();
        if (length == 0) {
            return chStr + ' ' + eventName + ": 0B";
        } else {
            return chStr + ' ' + eventName + ": " + length + 'B';
        }
    }

    /**
     * Generates the default log message of the specified event whose argument is a
     * {@link io.netty.buffer.ByteBufHolder}.
     */
    private static String formatByteBufHolder(ChannelHandlerContext ctx, String eventName, ByteBufHolder msg) {
        String chStr = ctx.channel().toString();
        String msgStr = msg.toString();
        ByteBuf content = msg.content();
        int length = content.readableBytes();
        if (length == 0) {
            return chStr + ' ' + eventName + ", " + msgStr + ", 0B";
        } else {
            return chStr + ' ' + eventName + ": " +
                    msgStr + ", " + length + 'B';
        }
    }

    /**
     * Generates the default log message of the specified event whose argument is an arbitrary object.
     */
    private static String formatSimple(ChannelHandlerContext ctx, String eventName, Object msg) {
        String chStr = ctx.channel().toString();
        String msgStr = String.valueOf(msg);
        return chStr + ' ' + eventName + ": " + msgStr;
    }
}
