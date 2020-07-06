package io.joyrpc.cluster.discovery.registry;

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

import io.joyrpc.cluster.discovery.config.Configure;
import io.joyrpc.cluster.discovery.naming.Registar;
import io.joyrpc.extension.URL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 注册中心接口
 */
public interface Registry extends Registar, Configure {

    /**
     * 打开
     *
     * @return 异步Future
     */
    CompletableFuture<Void> open();

    /**
     * 关闭
     *
     * @return 异步Future
     */
    CompletableFuture<Void> close();

    /**
     * 注册接口
     *
     * @param url url
     * @return 异步Future
     */
    CompletableFuture<URL> register(URL url);

    /**
     * 反注册接口
     *
     * @param url url
     * @return 异步Future
     */
    default CompletableFuture<URL> deregister(URL url) {
        return deregister(url, 0);
    }

    /**
     * 反注册接口
     *
     * @param url           url
     * @param maxRetryTimes 最大重试次数<br/>
     *                      <li>>0 最大重试次数</li>
     *                      <li>=0 不重试</li>
     *                      <li><0 无限重试</li>
     * @return 异步Future
     */
    CompletableFuture<URL> deregister(URL url, int maxRetryTimes);


    Supplier<Integer> ID_GENERATOR = new Supplier<Integer>() {

        protected AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Integer get() {
            return counter.incrementAndGet();
        }
    };

}
