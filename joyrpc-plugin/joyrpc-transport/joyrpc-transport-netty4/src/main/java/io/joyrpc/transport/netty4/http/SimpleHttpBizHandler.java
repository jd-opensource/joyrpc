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
import io.joyrpc.transport.http.*;
import io.joyrpc.transport.netty4.handler.NettyChannelContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * @date: 2019/2/14
 */
public class SimpleHttpBizHandler extends MessageToMessageDecoder<FullHttpRequest> {

    protected ChannelHandler channelHandler;
    protected Channel nettyChannel;

    public SimpleHttpBizHandler(ChannelHandler channelHandler, Channel nettyChannel) {
        this.channelHandler = channelHandler;
        this.nettyChannel = nettyChannel;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final FullHttpRequest msg, final List<Object> out) {
        //path
        String uri = msg.uri();
        //method
        HttpMethod method = HttpMethod.valueOf(msg.method().name());
        //headers
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        msg.headers().forEach(h -> {
            try {
                httpHeaders.set(h.getKey(), URLDecoder.decode(h.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException ignored) {
            }
        });
        //content
        byte[] content = method == HttpMethod.GET ? new byte[0] : getContentByMsg(msg);
        //创建HttpRequestMessage对象HttpRequestMessage
        HttpRequestMessage reqMsg = new DefaultHttpRequestMessage(uri, method, httpHeaders, content);
        //触发channlehandler
        channelHandler.received(new NettyChannelContext(nettyChannel), reqMsg);
    }

    protected byte[] getContentByMsg(final FullHttpRequest msg) {
        ByteBuf buf = msg.content();
        int size = buf.readableBytes();
        byte[] s1 = new byte[size];
        buf.readBytes(s1);
        return s1;
    }
}
