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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * 传输通道工厂类
 */
@Extensible("transportFactory")
public interface TransportFactory {

    /**
     * 构造传输通道客户端
     *
     * @param url URL
     * @return 客户端传输通道
     */
    TransportClient createClient(URL url);

    /**
     * 构造传输通道服务端
     *
     * @param url        URL
     * @param workerPool 业务线程池
     * @return 服务端传输通道
     */
    TransportServer createServer(URL url, ExecutorService workerPool);

    /**
     * 构造传输通道服务端
     *
     * @param url        URL
     * @param workerPool 业务线程池
     * @param beforeOpen 打开前
     * @param afterClose 打开后
     * @return 服务端传输通道
     */
    TransportServer createServer(URL url,
                                 ExecutorService workerPool,
                                 Function<TransportServer, CompletableFuture<Void>> beforeOpen,
                                 Function<TransportServer, CompletableFuture<Void>> afterClose);

}
