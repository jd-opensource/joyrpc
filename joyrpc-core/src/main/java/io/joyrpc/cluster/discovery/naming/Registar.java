package io.joyrpc.cluster.discovery.naming;

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

import io.joyrpc.cluster.Region;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 集群节点命名目录服务
 */
@Extensible("namingService")
public interface Registar extends Region {

    /**
     * 订阅接口(主要指对一个URL变化的订阅)
     *
     * @param url     the url
     * @param handler the listener
     * @return the boolean
     */
    boolean subscribe(URL url, ClusterHandler handler);

    /**
     * 取消订阅接口
     *
     * @param url     the url
     * @param handler
     * @return the boolean
     */
    boolean unsubscribe(URL url, ClusterHandler handler);

    /**
     * 获取目录服务URL
     *
     * @return
     */
    URL getUrl();

    Supplier<Integer> REGISTAR_ID_GENERATOR = new Supplier<Integer>() {

        protected AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Integer get() {
            return counter.incrementAndGet();
        }
    };

}
