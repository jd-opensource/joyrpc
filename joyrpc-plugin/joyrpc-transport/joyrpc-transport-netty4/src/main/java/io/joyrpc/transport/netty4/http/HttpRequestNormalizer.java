package io.joyrpc.transport.netty4.http;

import io.joyrpc.transport.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * 请求消息标准化
 */
public class HttpRequestNormalizer extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        ctx.fireChannelRead(msg instanceof FullHttpRequest ? normalize((FullHttpRequest) msg) : msg);
    }

    /**
     * 消息转换
     *
     * @param request 请求消息
     * @return 标准化的http请求消息
     */
    protected HttpRequestMessage normalize(final FullHttpRequest request) {
        //path
        String uri = request.uri();
        //method
        HttpMethod method = HttpMethod.valueOf(request.method().name());
        //headers
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        request.headers().forEach(h -> {
            try {
                httpHeaders.set(h.getKey(), URLDecoder.decode(h.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {
            }
        });
        byte[] content;
        if (method == HttpMethod.GET) {
            content = new byte[0];
        } else {
            ByteBuf buf = request.content();
            int size = buf.readableBytes();
            content = new byte[size];
            buf.readBytes(content);
        }
        //创建HttpRequestMessage对象HttpRequestMessage
        return new DefaultHttpRequestMessage(uri, method, httpHeaders, content);
    }
}
