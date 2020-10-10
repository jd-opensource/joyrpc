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
import io.joyrpc.transport.session.Session.RpcSession;

import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.RequestContext.*;
import static io.joyrpc.util.Maps.put;

/**
 * r请求上下文传递
 *
 * @date: 2019/6/14
 */
@Extension(value = "context", order = 1)
public class ContextTransmit implements Transmit {

    @Override
    public void inject(final RequestMessage<Invocation> request) {
        InnerContext context = new InnerContext(request.getContext());
        Invocation invocation = request.getPayLoad();
        //复制所有配置参数到invocation
        invocation.addAttachments(context.getRequests());
        invocation.addAttachments(context.getSessions());
        invocation.addAttachment(INTERNAL_KEY_TRACE, context.getTraces());
    }

    @Override
    public void restoreOnReceive(final RequestMessage<Invocation> request, final RpcSession session) {
        RequestContext context = request.getContext();
        context.setLocalAddress(request.getLocalAddress());
        context.setRemoteAddress(request.getRemoteAddress());

        Invocation invocation = request.getPayLoad();
        Map<String, Object> attachments = invocation.getAttachments();
        String remoteAppId = session == null ? null : session.getRemoteAppId();
        String remoteAppName = session == null ? null : session.getRemoteAppName();
        String remoteAppIns = session == null ? null : session.getRemoteAppIns();
        //获取待写入数据条数
        int attachmentSize = attachments != null ? attachments.size() : 0;
        int sessionSize = remoteAppId != null || remoteAppName != null ? 3 : 0;
        int size = attachmentSize + sessionSize;
        if (size == 0) {
            return;
        }
        InnerContext ctx = new InnerContext(context);
        Map<String, Object> callers = new HashMap<>(size);
        //写入透传的数据
        if (attachmentSize > 0) {
            ctx.setTraces((Map<String, Object>) attachments.remove(INTERNAL_KEY_TRACE));
            //保留重试次数，便于兼容
            Integer retryTimes = invocation == null ? null : invocation.getAttachment(INTERNAL_KEY_RETRY_TIMES);
            if (retryTimes != null) {
                request.setRetryTimes(retryTimes);
            }
            //复制内部属性，转换成隐藏属性
            attachments.forEach((k, v) -> callers.put(INTERNAL_KEY.test(k) ? INTERNAL_TO_HIDDEN.apply(k) : k, v));
        }
        //优先使用会话里面值，防止透传
        if (sessionSize > 0) {
            put(callers, HIDDEN_KEY_APPID, remoteAppId);
            put(callers, HIDDEN_KEY_APPNAME, remoteAppName);
            put(callers, HIDDEN_KEY_APPINSID, remoteAppIns);
        }
        if (!callers.isEmpty()) {
            ctx.setCallers(callers);
        }
    }

}
