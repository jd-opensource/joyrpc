package io.joyrpc.protocol.handler;

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

import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.protocol.MessageHandler;
import io.joyrpc.transport.channel.SendResult;
import io.joyrpc.transport.message.Message;
import io.joyrpc.util.network.Ipv4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 认证处理器
 */
public abstract class AbstractReqHandler implements MessageHandler {

    protected final static Logger logger = LoggerFactory.getLogger(AbstractReqHandler.class);

    /**
     * 发送异常消费者
     */
    protected Consumer<SendResult> sendFailed = (event) -> {
        if (!event.isSuccess()) {
            Throwable throwable = event.getThrowable();
            getLogger().error(String.format(" %s Failed to send error to remote %s for message id: %s Cause by:",
                    ExceptionCode.format(ExceptionCode.PROVIDER_SEND_MESSAGE_ERROR),
                    Ipv4.toAddress(event.getRemoteAddress()),
                    event.getRequest() != null ? String.valueOf(((Message) event.getRequest()).getMsgId()) : null),
                    throwable);
        }
    };

    /**
     * 获取日志
     *
     * @return
     */
    protected Logger getLogger() {
        return logger;
    }

}
