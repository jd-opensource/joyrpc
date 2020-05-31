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
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.TraceFactory;
import io.joyrpc.trace.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.TRACE_FACTORY;
import static io.joyrpc.constants.Constants.*;

/**
 * 抽象的跟踪插件,标签的名称和业界通用叫法一致
 */
public abstract class AbstractTraceFilter extends AbstractFilter {

    protected static final String CLIENT_ALIAS_TAG = "client.alias";
    protected static final String CLIENT_NAME_TAG = "client.name";
    protected static final String CLIENT_ADDRESS_TAG = "client.address";
    protected static final String SERVER_ADDRESS_TAG = "server.address";
    protected static final String SPAN_KIND_TAG = "span.kind";
    protected static final String COMPONENT_TAG = "component";
    /**
     * 组件名称
     */
    protected String component;
    /**
     * 跟踪工厂类
     */
    protected TraceFactory factory;
    /**
     * 接口开启注解
     */
    protected EnableTrace enableTrace;
    /**
     * 是否开启
     */
    protected boolean enable;

    @Override
    public void setup() {
        MapParametric parametric = new MapParametric(GlobalContext.getContext());
        factory = TRACE_FACTORY.getOrDefault(parametric.getString(TRACE_TYPE));
        component = parametric.getString(PROTOCOL_KEY);
        enableTrace = (EnableTrace) clazz.getAnnotation(EnableTrace.class);
        enable = factory != null && (enableTrace == null || enableTrace.value());
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        if (enable) {
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
        return invoker.invoke(request);
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
        MapParametric parametric = new MapParametric(GlobalContext.getContext());
        //是配置了开关
        if (url.getBoolean(TRACE_OPEN, parametric.getBoolean(TRACE_OPEN, Boolean.FALSE))) {
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
