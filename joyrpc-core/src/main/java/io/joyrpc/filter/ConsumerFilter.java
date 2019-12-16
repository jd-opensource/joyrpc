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

import io.joyrpc.extension.Extensible;

/**
 * 消费者过滤器接口
 */
@Extensible("consumerFilter")
@FunctionalInterface
public interface ConsumerFilter extends Filter {

    /**
     * 跟踪顺序
     */
    int TRACE_ORDER = -110;

    /**
     * 参数验证顺序，泛型调用不会进行验证
     */
    int VALIDATION_ORDER = -100;

    /**
     * 泛化处理器的顺序，需要在缓存和Mock之前，便于对数据做处理
     */
    int GENERIC_ORDER = -90;

    /**
     * 并发控制顺序
     */
    int CONCURRENCY_ORDER = -80;
    /**
     * 缓存处理器顺序
     */
    int CACHE_ORDER = -70;

    /**
     * Mock处理器顺序
     */
    int MOCK_ORDER = -60;

}
