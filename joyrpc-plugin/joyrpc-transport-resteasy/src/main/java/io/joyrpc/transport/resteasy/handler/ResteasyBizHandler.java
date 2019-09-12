package io.joyrpc.transport.resteasy.handler;

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

import io.joyrpc.context.RequestContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import org.jboss.resteasy.plugins.server.netty.NettyHttpRequest;
import org.jboss.resteasy.plugins.server.netty.NettyHttpResponse;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.spi.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Resteasy业务处理器
 */
public class ResteasyBizHandler extends SimpleChannelInboundHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResteasyBizHandler.class);

    protected RequestDispatcher dispatcher;

    public ResteasyBizHandler(RequestDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof NettyHttpRequest) {

            NettyHttpRequest request = (NettyHttpRequest) msg;

            if (request.getUri().getPath().endsWith("/favicon.ico")) {
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
                ctx.writeAndFlush(response);
                return;
            }

            if (request.is100ContinueExpected()) {
                send100Continue(ctx);
            }

            NettyHttpResponse response = request.getResponse();
            try {
                // 获取远程ip 兼容nignx转发和vip等
                HttpHeaders httpHeaders = request.getHttpHeaders();
                String remoteip = httpHeaders.getHeaderString("X-Forwarded-For");
                if (remoteip == null) {
                    remoteip = httpHeaders.getHeaderString("X-Real-IP");
                }
                if (remoteip != null) {
                    RequestContext.getContext().setRemoteAddress(InetSocketAddress.createUnresolved(remoteip, 0));
                } else { // request取不到就从channel里取
                    RequestContext.getContext().setRemoteAddress((InetSocketAddress) ctx.channel().remoteAddress());
                }
                // 设置本地地址
                RequestContext.getContext().setLocalAddress((InetSocketAddress) ctx.channel().localAddress());

                dispatcher.service(request, response, true);
            } catch (Failure e1) {
                response.reset();
                response.setStatus(e1.getErrorCode());
            } catch (Exception ex) {
                response.reset();
                response.setStatus(500);
                logger.error("Unexpected", ex);
            }

            if (!request.getAsyncContext().isSuspended()) {
                response.finish();
                ctx.flush();
            }
        }
    }

    private void send100Continue(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        if (e.getCause() instanceof TooLongFrameException) {
            DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, REQUEST_ENTITY_TOO_LARGE);
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            if (ctx.channel().isActive()) { // 连接已断开就不打印了
                logger.warn("Exception caught by request handler", e);
            }
            ctx.close();
        }
    }
}
