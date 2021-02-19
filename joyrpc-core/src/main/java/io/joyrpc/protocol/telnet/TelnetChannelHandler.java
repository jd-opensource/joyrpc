package io.joyrpc.protocol.telnet;

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

import io.joyrpc.exception.ParserException;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.channel.ChannelOperator;
import io.joyrpc.transport.telnet.TelnetHandler;
import io.joyrpc.transport.telnet.TelnetRequest;
import io.joyrpc.transport.telnet.TelnetResponse;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.joyrpc.Plugin.TELNET_HANDLER;
import static io.joyrpc.transport.telnet.TelnetHandler.LINE;


/**
 * Telnet处理器
 */
public class TelnetChannelHandler implements ChannelOperator {

    /**
     * slf4j Logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(TelnetChannelHandler.class);


    @Override
    public void inactive(final ChannelContext context) throws Exception {
        logger.info("Disconnected telnet from " + Channel.toString(context.getChannel()));
        context.fireChannelInactive();
    }

    @Override
    public void received(final ChannelContext context, final Object obj) throws Exception {
        TelnetResponse response;
        Channel channel = context.getChannel();
        if (obj instanceof TelnetResponse) {
            response = (TelnetResponse) obj;
            channel.send(response.getResponse(), response.getConsumer());
        } else if (obj instanceof TelnetRequest) {
            TelnetRequest request = (TelnetRequest) obj;
            //查找命令插件
            TelnetHandler handler = TELNET_HANDLER.get(request.getCmd());
            if (handler != null) {
                //调用指定命令
                try {
                    response = handler.telnet(channel, request.getArgs());
                    if (handler.newLine()) {
                        response.getBuilder().insert(0, LINE);
                    }
                } catch (ParserException e) {
                    //解析命令参数异常
                    response = new TelnetResponse(new StringBuilder(1024).append(LINE).append("ERROR:")
                            .append(e.getMessage()).append(LINE).append(handler.help()));
                } catch (IllegalArgumentException e) {
                    //参数异常
                    response = new TelnetResponse(new StringBuilder(1024).append(LINE).append("ERROR:")
                            .append(StringUtils.toString(e)).append(LINE).append(handler.help()));
                } catch (Throwable e) {
                    //其它异常
                    logger.error("Error occurs while executing " + request.getCmd(), e);
                    response = new TelnetResponse(new StringBuilder(1024).append(LINE).append("ERROR:")
                            .append(StringUtils.toString(e)).append(LINE).append(handler.help()));
                }
            } else {
                response = new TelnetResponse(new StringBuilder(100).append(LINE)
                        .append("ERROR:You input the command:[").append(request.getCmd()).append("] is not exist!!"));
            }
            if (!response.isEmpty()) {
                response.getBuilder().append(LINE).append(request.getPrompt());
                channel.send(response.getResponse(), response.getConsumer());
            }
        }
    }

    @Override
    public void caught(final ChannelContext context, final Throwable throwable) {
        logger.error("Error occurs while handling telnet command.", throwable);
        context.getChannel().send(throwable.getMessage(), r -> {
            if (!r.isSuccess()) {
                logger.error(String.format("Error occurs while sending telnet command throwable message -- %s", throwable.getMessage()));
            }
        });
    }


}
