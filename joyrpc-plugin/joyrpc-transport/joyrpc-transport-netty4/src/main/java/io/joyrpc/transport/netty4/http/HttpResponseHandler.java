package io.joyrpc.transport.netty4.http;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandler;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpResponseMessage;
import io.joyrpc.transport.netty4.handler.NettyChannelContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * http应答处理器
 */
public class HttpResponseHandler extends ChannelOutboundHandlerAdapter {

    protected ChannelHandler handler;
    protected Channel channel;

    public HttpResponseHandler(ChannelHandler handler, Channel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        Object result = handler.wrote(new NettyChannelContext(channel), msg);
        if (result instanceof HttpResponseMessage) {
            HttpResponseMessage message = (HttpResponseMessage) result;
            //获取content
            //TODO 是否要拷贝？还是直接wrapper
            ByteBuf content = Unpooled.copiedBuffer(message.content());
            //获取status
            HttpResponseStatus status = HttpResponseStatus.valueOf(message.getStatus());
            //创建FullHttpResponse对象
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, status, content);
            //设置header
            HttpHeaders headers = message.headers();
            headers.getAll().forEach((k, v) -> res.headers().set(k, v));
            //设置消息长度
            HttpUtil.setContentLength(res, content.readableBytes());
            //发送
            ChannelFuture f = ctx.writeAndFlush(res);
            if (!headers.isKeepAlive()) {
                HttpUtil.setKeepAlive(res, false);
                f.addListener(ChannelFutureListener.CLOSE);
            } else {
                HttpUtil.setKeepAlive(res, true);
            }
        } else {
            super.write(ctx, result, promise);
        }
    }
}
