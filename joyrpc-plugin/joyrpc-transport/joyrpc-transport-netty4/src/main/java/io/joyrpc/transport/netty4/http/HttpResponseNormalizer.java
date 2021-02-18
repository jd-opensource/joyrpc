package io.joyrpc.transport.netty4.http;

import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 应答消息标准化
 */
public class HttpResponseNormalizer extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponseMessage) {
            //创建FullHttpResponse对象
            FullHttpResponse response = normalize((HttpResponseMessage) msg);
            //发送
            ChannelFuture future = ctx.writeAndFlush(response);
            if (HttpHeaderValues.KEEP_ALIVE.contentEqualsIgnoreCase(response.headers().get(HttpHeaderNames.CONNECTION))) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            ctx.write(msg, promise);
        }
    }

    /**
     * 消息转换
     *
     * @param message 应答消息
     * @return Netty的应答消息
     */
    protected FullHttpResponse normalize(final HttpResponseMessage message) {
        //获取content
        ByteBuf content = Unpooled.wrappedBuffer(message.content());
        //获取status
        HttpResponseStatus status = HttpResponseStatus.valueOf(message.getStatus());
        //创建FullHttpResponse对象
        FullHttpResponse result = new DefaultFullHttpResponse(HTTP_1_1, status, content);
        //设置header
        HttpHeaders headers = message.headers();
        headers.getAll().forEach((k, v) -> result.headers().set(k, v));
        //设置消息长度
        HttpUtil.setContentLength(result, content.readableBytes());
        HttpUtil.setKeepAlive(result, headers.isKeepAlive());
        return result;
    }
}
