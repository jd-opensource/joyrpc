package io.joyrpc.cluster.event;

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

import io.joyrpc.event.AbstractEvent;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.message.Message;

/**
 * 集群指标事件
 */
public class MetricEvent extends AbstractEvent {
    //集群URL
    protected final URL cluster;
    //集群名
    protected final String clusterName;
    //目标节点URL
    protected final URL url;
    //请求
    protected final Message request;
    //应答
    protected final Message response;
    //当前并发数
    protected final int concurrency;
    //开始时间
    protected final long startTime;
    //结束时间
    protected final long endTime;
    //异常
    protected final Throwable throwable;

    /**
     * 构造函数
     *
     * @param source
     * @param target
     * @param cluster
     * @param url
     * @param request
     * @param response
     * @param throwable
     * @param concurrency
     * @param startTime
     * @param endTime
     */
    public MetricEvent(final Object source, final Object target,
                       final URL cluster, final String clusterName, final URL url,
                       final Message request, final Message response, final Throwable throwable,
                       final int concurrency, final long startTime, final long endTime) {
        super(source, target);
        this.cluster = cluster;
        this.clusterName = clusterName;
        this.url = url;
        this.request = request;
        this.response = response;
        this.concurrency = concurrency;
        this.startTime = startTime;
        this.endTime = endTime;
        this.throwable = throwable;
    }

    public URL getCluster() {
        return cluster;
    }

    public String getClusterName() {
        return clusterName;
    }

    public URL getUrl() {
        return url;
    }

    public Message getRequest() {
        return request;
    }

    public Message getResponse() {
        return response;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
