package io.joyrpc.filter;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.annotation.EnableTrace;
import io.joyrpc.config.InterfaceOption;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.TraceFactory;
import io.joyrpc.trace.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.TRACE_FACTORY;
import static io.joyrpc.constants.Constants.PROTOCOL_KEY;
import static io.joyrpc.constants.Constants.TRACE_OPEN;

/**
 * 抽象的跟踪插件
 */
public abstract class AbstractTraceFilter extends AbstractFilter {

    protected static final String CONSUMER_ALIAS_TAG = "consumer.alias";
    protected static final String CONSUMER_NAME_TAG = "consumer.name";
    protected static final String CONSUMER_ADDRESS_TAG = "consumer.address";
    protected static final String PROVIDER_ADDRESS_TAG = "provider.address";
    protected static final String SPAN_KIND_TAG = "span.kind";
    protected static final String COMPONENT_TAG = "component";
    /**
     * 组件名称
     */
    protected String component;
    protected TraceFactory factory;
    /**
     * 接口开启注解
     */
    protected EnableTrace enableTrace;
    /**
     * 全局开关
     */
    protected boolean enable;

    @Override
    public void setup() {
        factory = TRACE_FACTORY.get();
        component = GlobalContext.getString(PROTOCOL_KEY);
        enableTrace = (EnableTrace) clazz.getAnnotation(EnableTrace.class);
        enable = GlobalContext.getBoolean(TRACE_OPEN, Boolean.FALSE);
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        InterfaceOption.MethodOption option = request.getOption();
        Map<String, String> tags = new HashMap<>();
        createTags(request, tags);
        Tracer trace = factory.create(request);
        trace.begin(option.getTraceSpanId(invocation), component, tags);
        trace.snapshot();
        CompletableFuture<Result> future = invoker.invoke(request);
        future.whenComplete((result, throwable) -> {
            trace.restore();
            trace.end(throwable == null ? result.getException() : throwable);
        });
        return future;
    }

    /**
     * 打标签
     *
     * @param request 请求
     * @param tags    标签
     */
    protected void createTags(final RequestMessage<Invocation> request, final Map<String, String> tags) {
    }

    @Override
    public boolean test(final URL url) {
        if (factory == null) {
            return false;
        } else if (enableTrace != null && !enableTrace.value()) {
            //该接口声明不支持
            return false;
        } else if (url.getBoolean(TRACE_OPEN, enableTrace == null ? enable : true)) {
            //接口配置
            return true;
        }
        Map<String, String> tokens = url.endsWith("." + TRACE_OPEN);
        if (tokens.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (Boolean.parseBoolean(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
