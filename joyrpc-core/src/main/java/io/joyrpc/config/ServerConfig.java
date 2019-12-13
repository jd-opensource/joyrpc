package io.joyrpc.config;

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


import io.joyrpc.config.validator.ValidatePlugin;
import io.joyrpc.constants.Constants;
import io.joyrpc.thread.ThreadPool;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.transport.TransportFactory;
import io.joyrpc.util.network.Ipv4;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.constants.Constants.*;


/**
 * 服务配置
 */
public class ServerConfig extends AbstractIdConfig implements Serializable {
    /**
     * 最大IO的buffer大小
     */
    public final static int MAX_BUFFER_SIZE = 32 * 1024;
    /**
     * 最小IO的buffer大小
     */
    public final static int MIN_BUFFER_SIZE = 1 * 1024;
    /**
     * 默认启动端口，包括不配置或者随机，都从此端口开始计算
     */
    public static final int DEFAULT_SERVER_PORT = 22000;
    /**
     * 实际监听IP，与网卡对应
     */
    protected String host;
    /**
     * 监听端口
     */
    @Min(value = Ipv4.MIN_USER_PORT, message = "port must be between " + Ipv4.MIN_USER_PORT + " and " + Ipv4.MAX_USER_PORT)
    @Max(value = Ipv4.MAX_USER_PORT, message = "port must be between " + Ipv4.MIN_USER_PORT + " and " + Ipv4.MAX_USER_PORT)
    protected Integer port;
    /**
     * 基本路径 默认"/"
     */
    protected String contextPath;
    /**
     * io线程池大小
     */
    protected Integer ioThreads;
    /**
     * 业务线程池类型
     */
    @ValidatePlugin(extensible = ThreadPool.class, name = "THREAD_POOL", defaultValue = DEFAULT_THREADPOOL)
    protected String threadPool;
    /**
     * 业务线程池core大小
     */
    protected Integer coreThreads;
    /**
     * 业务线程池max大小
     */
    protected Integer maxThreads;
    /**
     * 业务线程池队列类型
     */
    protected String queueType;
    /**
     * 业务线程池队列大小
     */
    protected Integer queues;
    /**
     * 服务端允许客户端建立的连接数
     */
    @Min(value = 1, message = "accepts must be greater than 0")
    protected Integer accepts;
    /**
     * IO的buffer大小
     */
    protected Integer buffers;
    /**
     * 是否启动epoll，
     */
    protected Boolean epoll;
    /**
     * 客户端和服务端工厂插件
     */
    @ValidatePlugin(extensible = EndpointFactory.class, name = "ENDPOINT_FACTORY", defaultValue = DEFAULT_ENDPOINT_FACTORY)
    protected String endpointFactory;
    /**
     * 传输实现工厂插件
     */
    @ValidatePlugin(extensible = TransportFactory.class, name = "TRANSPORT_FACTORY", defaultValue = DEFAULT_TRANSPORT_FACTORY)
    protected String transportFactory;
    /**
     * The Parameters. 自定义参数
     */
    protected Map<String, String> parameters;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public Integer getIoThreads() {
        return ioThreads;
    }

    public void setIoThreads(Integer ioThreads) {
        this.ioThreads = ioThreads;
    }

    public Integer getCoreThreads() {
        return coreThreads;
    }

    public void setCoreThreads(Integer coreThreads) {
        this.coreThreads = coreThreads;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(Integer maxThreads) {
        this.maxThreads = maxThreads;
    }

    public String getQueueType() {
        return queueType;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public String getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(String threadPool) {
        this.threadPool = threadPool;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public Boolean getEpoll() {
        return epoll;
    }

    public void setEpoll(Boolean epoll) {
        this.epoll = epoll;
    }

    public void setParameter(final String key, final String value) {
        if (parameters == null) {
            parameters = new HashMap<>();
        }
        if (key == null || value == null) {
            return;
        }
        parameters.put(key, value);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Integer getBuffers() {
        return buffers;
    }

    public void setBuffers(Integer bufferSize) {
        if (buffers == null) {
            return;
        }
        if (bufferSize > MAX_BUFFER_SIZE) {
            this.buffers = MAX_BUFFER_SIZE;
        } else if (bufferSize < MIN_BUFFER_SIZE) {
            this.buffers = MIN_BUFFER_SIZE;
        } else {
            this.buffers = bufferSize;
        }
    }

    public String getTransportFactory() {
        return transportFactory;
    }

    public void setTransportFactory(String transportFactory) {
        this.transportFactory = transportFactory;
    }

    public String getEndpointFactory() {
        return endpointFactory;
    }

    public void setEndpointFactory(String endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    @Override
    public int hashCode() {
        final Integer prime = 31;
        Integer result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ServerConfig other = (ServerConfig) obj;
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ServerConfig [port=" + port + ", host=" + host + "]";
    }

    @Override
    protected Map<String, String> addAttribute2Map(final Map<String, String> params) {
        super.addAttribute2Map(params);
        //TODO CONTEXT_PATH_OPTION好像没地方使用
        addElement2Map(params, Constants.CONTEXT_PATH_OPTION, contextPath);
        addElement2Map(params, Constants.IO_THREAD_OPTION, ioThreads);
        addElement2Map(params, Constants.BOSS_THREAD_OPTION, null);
        addElement2Map(params, Constants.CORE_SIZE_OPTION, coreThreads);
        addElement2Map(params, Constants.MAX_SIZE_OPTION, maxThreads);
        addElement2Map(params, Constants.THREADPOOL_OPTION, threadPool);
        addElement2Map(params, Constants.QUEUE_TYPE_OPTION, queueType);
        addElement2Map(params, Constants.QUEUES_OPTION, queues);
        addElement2Map(params, Constants.BUFFER_OPTION, buffers);
        addElement2Map(params, Constants.EPOLL_OPTION, epoll);
        addElement2Map(params, Constants.CONNECTION_ACCEPTS, accepts);
        addElement2Map(params, Constants.ENDPOINT_FACTORY_OPTION, endpointFactory);
        addElement2Map(params, Constants.TRANSPORT_FACTORY_OPTION, transportFactory);
        if (null != parameters) {
            parameters.entrySet().forEach(entry -> {
                addElement2Map(params, entry.getKey(), entry.getValue());
            });
        }
        return params;

    }
}
