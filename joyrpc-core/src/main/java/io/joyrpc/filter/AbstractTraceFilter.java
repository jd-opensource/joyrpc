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
import io.joyrpc.invoker.option.InterfaceOption;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.option.MethodOption;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.TraceFactory;
import io.joyrpc.trace.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.Plugin.TRACE_FACTORY;
import static io.joyrpc.constants.Constants.PROTOCOL_KEY;
import static io.joyrpc.constants.Constants.TRACE_TYPE;
import static io.joyrpc.context.Variable.VARIABLE;

/**
 * 抽象的跟踪插件,标签的名称和业界通用叫法一致
 */
public abstract class AbstractTraceFilter extends AbstractFilter {

    protected static final String CLIENT_ALIAS_TAG = "client.alias";
    protected static final String CLIENT_NAME_TAG = "client.name";
    protected static final String CLIENT_ADDRESS_TAG = "client.address";
    protected static final String CLIENT_RETRY_TAG = "client.retry";
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

    @Override
    public void setup() {
        component = GlobalContext.getString(PROTOCOL_KEY);
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        if (factory == null) {
            return invoker.invoke(request);
        }
        Invocation invocation = request.getPayLoad();
        MethodOption option = request.getOption();
        if (!option.isTrace()) {
            return invoker.invoke(request);
        }
        //构造跟踪标签
        Map<String, String> tags = new HashMap<>();
        createTags(request, tags);
        //创建跟踪
        Tracer trace = factory.create(request);
        //启动跟踪
        trace.begin(option.getTraceSpanId(invocation), component, tags);
        //快照
        trace.snapshot();
        //远程调用
        CompletableFuture<Result> future = invoker.invoke(request);
        //主线程调用结束
        trace.prepare();
        future.whenComplete((result, throwable) -> {
            //异步线程恢复
            trace.restore();
            //异步线程结束
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
        return false;
    }

    @Override
    public boolean test(final InterfaceOption option) {
        if (!option.isTrace()) {
            return false;
        }
        factory = TRACE_FACTORY.get(VARIABLE.getString(TRACE_TYPE));
        return factory != null;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }

}
