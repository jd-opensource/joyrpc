package io.joyrpc.transport;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;
import io.joyrpc.thread.ThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * 端点工厂类
 */
@Extensible("endpointFactory")
public interface EndpointFactory {

    /**
     * 创建客户端
     *
     * @param url        URL
     * @param workerPool 线程池
     * @param function   客户端函数
     * @return 客户端
     */
    Client createClient(URL url, ThreadPool workerPool, Function<TransportClient, Client> function);

    /**
     * 创建服务端
     *
     * @param url        url
     * @param workerPool 业务线程池
     * @return 服务端
     */
    Server createServer(URL url, ThreadPool workerPool);
}
