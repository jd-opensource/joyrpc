package io.joyrpc.transport.netty4.ssl;

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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2019/8/13
 */
public class SslServerHandshakeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SslServerHandshakeHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                (GenericFutureListener<Future<Channel>>) future -> {
                    if (future.isSuccess()) {
                        logger.info("Ssl handshake is successed, session is protected by " + ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite());
                    } else {
                        ctx.channel().close();
                    }
                });
        ctx.fireChannelActive();
    }

}
