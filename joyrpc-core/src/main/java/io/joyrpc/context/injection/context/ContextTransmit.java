package io.joyrpc.context.injection.context;

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
import io.joyrpc.context.injection.Transmit;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.session.DefaultSession;

import java.util.Map;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.RequestContext.*;

/**
 * r请求上下文传递
 *
 * @date: 2019/6/14
 */
@Extension(value = "context", order = 1)
public class ContextTransmit implements Transmit {

    @Override
    public void inject(final RequestMessage<Invocation> request) {
        RequestContext context = request.getContext();
        Invocation invocation = request.getPayLoad();
        //复制所有配置参数到invocation
        invocation.addAttachments(context.getAttachments());
        //会话
        invocation.addAttachment(HIDDEN_KEY_SESSION, context.getSession());
        //超时时间
        Object requestTimeout = context.getAttachment(HIDDEN_KEY_TIME_OUT);
        if (requestTimeout != null) {
            context.removeAttachment(HIDDEN_KEY_TIME_OUT);
            request.getHeader().setTimeout(requestTimeout instanceof Integer ? (Integer) requestTimeout : Integer.valueOf(requestTimeout.toString()));
        }
    }

    @Override
    public void restore(final RequestMessage<Invocation> request, final DefaultSession session) {
        RequestContext context = request.getContext();
        Map<String, Object> attachments = request.getPayLoad().getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            //复制所有非内部属性，内部处理了HIDDEN_KEY_SESSION
            context.setAttachments(attachments, NONE_INTERNAL_KEY.and((o) -> !GENERIC_OPTION.getName().equals(o)));
            //复制内部属性，转换成隐藏属性
            attachments.forEach((k, v) -> {
                if (INTERNAL_KEY.test(k)) {
                    context.setAttachment(INTERNAL_TO_HIDDEN.apply(k), v, o -> true);
                }
            });
        }
        //优先使用会话里面值，防止透传
        if (session != null && session.getRemoteAppId() != null) {
            context.setAttachment(HIDDEN_KEY_APPID, session.getRemoteAppId());
            context.setAttachment(HIDDEN_KEY_APPNAME, session.getRemoteAppName());
            context.setAttachment(HIDDEN_KEY_APPINSID, session.getRemoteAppIns());
        }
    }
}
