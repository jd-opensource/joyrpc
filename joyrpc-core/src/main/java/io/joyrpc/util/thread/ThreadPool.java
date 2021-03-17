package io.joyrpc.util.thread;

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

import io.joyrpc.extension.Parametric;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 线程池
 */
public interface ThreadPool extends ExecutorService {

    /**
     * 输出运行时信息
     *
     * @return 运行时信息
     */
    Map<String, Object> dump();

    /**
     * 配置参数
     *
     * @param parametric 参数
     */
    void configure(Parametric parametric);
}
