package io.joyrpc.protocol.http.handler;

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

import io.joyrpc.extension.URL;
import io.joyrpc.protocol.http.HeaderMapping;
import io.joyrpc.protocol.http.HttpController;
import io.joyrpc.protocol.http.controller.DefaultHttpController;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelReader;
import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpMethod;
import io.joyrpc.transport.http.HttpRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static io.joyrpc.protocol.http.HeaderMapping.KEEP_ALIVE;
import static io.joyrpc.protocol.http.Plugin.CONTENT_TYPE_HANDLER;
import static io.joyrpc.protocol.http.Plugin.HTTP_CONTROLLER;

/**
 * HTTP转换成joy
 */
public class HttpToJoyHandler implements ChannelReader {

    private final static Logger logger = LoggerFactory.getLogger(HttpToJoyHandler.class);

    /**
     * 默认序列化器
     */
    protected HttpController defController;

    public HttpToJoyHandler() {
        defController = new DefaultHttpController();
    }

    @Override
    public void received(final ChannelContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequestMessage) {
            HttpRequestMessage message = (HttpRequestMessage) msg;
            HttpMethod method = message.getHttpMethod();
            String uri = message.getUri();
            switch (method) {
                case GET:
                case POST:
                case PUT:
                    break;
                default:
                    ctx.writeAndFlush(error("Only allow GET POST and PUT", message.headers().isKeepAlive()));
                    return;
            }
            // 相对路径，确保以"/"开头
            if (!uri.startsWith("/")) {
                uri = "/" + uri;
            }
            // 解析uri
            List<String> params = new LinkedList<>();
            String host = (String) message.headers().get(HttpHeaders.Names.HOST);
            URL url = URL.valueOf(host + uri, "http", params);
            // 根据协议调用插件
            String contentType = (String) message.headers().get(HttpHeaders.Names.CONTENT_TYPE);
            try {
                HttpController controller = contentType == null || contentType.isEmpty() ? null : CONTENT_TYPE_HANDLER.get(contentType);
                if (controller != null) {
                    ctx.writeAndFlush(controller.execute(ctx, message, url, params));
                } else {
                    // 根据路径调用插件
                    String path = url.getAbsolutePath();
                    int pos = path.indexOf('/', 1);
                    //获取插件
                    controller = HTTP_CONTROLLER.get(pos > 0 ? path.substring(0, pos) : path);
                    if (controller != null) {
                        ctx.writeAndFlush(controller.execute(ctx, message, !controller.relativePath() ? url : url.setPath(path.substring(pos + 1)), params));
                    } else {
                        ctx.writeAndFlush(defController.execute(ctx, message, url, params));
                    }
                }
            } catch (Throwable e) {
                // 解析请求body
                logger.error(String.format("Error occurs while parsing http request for uri %s from %s", uri, Channel.toString(ctx.getChannel().getRemoteAddress())), e);
                //write error msg back
                ctx.writeAndFlush(error(e.getMessage(), message.headers().isKeepAlive()));
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * 回写数据
     *
     * @param error       异常消息
     * @param isKeepAlive 保持连接
     */
    protected Object error(final String error, final boolean isKeepAlive) {
        ResponseMessage<ResponsePayload> response = new ResponseMessage<>();
        response.getHeader()
                .addAttribute(HeaderMapping.CONTENT_TYPE.getNum(), "text/json; charset=UTF-8")
                .addAttribute(KEEP_ALIVE.getNum(), isKeepAlive);
        response.setPayLoad(new ResponsePayload(new Exception(error)));
        return response;
    }

}
