package io.joyrpc.metric.mc;

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

import io.joyrpc.cluster.distribution.CircuitBreaker;
import io.joyrpc.cluster.event.MetricEvent;
import io.joyrpc.invoker.option.ConsumerMethodOption;
import io.joyrpc.extension.URL;
import io.joyrpc.metric.Clock;
import io.joyrpc.metric.Dashboard;
import io.joyrpc.metric.TPWindow;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.transport.message.Message;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.joyrpc.constants.Constants.METRIC_WINDOWS_TIME_OPTION;

/**
 * 基于方法统计的指标实现
 */
public class McDashboard implements Dashboard {
    /**
     * 集群URL
     */
    protected URL url;
    /**
     * 指标
     */
    protected TPWindow window;
    /**
     * 方法指标
     */
    protected Map<String, TPWindow> methods = new ConcurrentHashMap<>();
    /**
     * 类型
     */
    protected Dashboard.DashboardType type;
    /**
     * 时间窗口间隔
     */
    protected long interval;

    /**
     * 构造函数
     *
     * @param url
     * @param type
     */
    public McDashboard(final URL url, final DashboardType type) {
        this.url = url;
        this.type = type;
        this.interval = url.getPositiveLong(METRIC_WINDOWS_TIME_OPTION);
        this.window = new McTPWindow(interval, Clock.MILLI);
    }

    @Override
    public TPWindow getMetric() {
        return window;
    }

    @Override
    public void snapshot() {
        List<TPWindow> windows = new LinkedList<>();
        windows.add(window);
        for (Map.Entry<String, TPWindow> entry : methods.entrySet()) {
            windows.add(entry.getValue());
        }
        windows.forEach(o -> o.snapshot());
    }

    /**
     * 获取方法的性能指标
     *
     * @param methodName
     * @return
     */
    public TPWindow getMethod(final String methodName) {
        return methodName == null ? null : methods.computeIfAbsent(methodName, o -> new McTPWindow(interval, Clock.MILLI));
    }

    @Override
    public void handle(final MetricEvent event) {
        Message message = event.getRequest();
        if (message instanceof RequestMessage) {
            RequestMessage request = (RequestMessage) message;
            Object payload = request.getPayLoad();
            if (payload instanceof Invocation) {
                onInvocation(event, request);
            }
        }
    }

    /**
     * 方法调用
     *
     * @param event   事件
     * @param request 请求
     */
    protected void onInvocation(final MetricEvent event, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        ConsumerMethodOption option = (ConsumerMethodOption) request.getOption();
        //方法的指标
        TPWindow method = getMethod(invocation.getMethodName());
        Throwable throwable = getThrowable(event);
        if (throwable != null) {
            //如果有异常，进行异常统计
            if (type == DashboardType.Node) {
                //只有节点才触发熔断逻辑，集群也会收到相同的事件不进行处理
                //判断熔断支持的异常才统计数据
                method.failure();
                window.failure();
                CircuitBreaker breaker = option.getCircuitBreaker();
                if (breaker != null && breaker.support(throwable)) {
                    //触发熔断
                    breaker.apply(throwable, method);
                }
            }
        } else if (event.getStartTime() > 0 && event.getEndTime() > 0) {
            //如果正常执行，统计成功
            int elapse = (int) (event.getEndTime() - event.getStartTime());
            method.success(elapse);
            method.actives().set(event.getConcurrency());
            window.success(elapse);
            window.actives().set(event.getConcurrency());
        }
    }

    /**
     * 获取异常
     *
     * @param event
     * @return
     */
    protected Throwable getThrowable(final MetricEvent event) {
        if (event.getThrowable() != null) {
            return event.getThrowable();
        }
        Message message = event.getResponse();
        if (message instanceof ResponseMessage) {
            ResponseMessage responseMessage = ((ResponseMessage) message);
            Object payLoad = responseMessage.getPayLoad();
            if (payLoad instanceof ResponsePayload) {
                ResponsePayload responsePayload = ((ResponsePayload) payLoad);
                if (responsePayload.isError()) {
                    return responsePayload.getException();
                }
            }
        }
        return null;
    }

}
