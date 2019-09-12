package io.joyrpc.thread;

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

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.joyrpc.constants.Constants.QUEUES_OPTION;
import static io.joyrpc.constants.Constants.QUEUE_TYPE_OPTION;

/**
 * 线程池
 */
@Extensible("threadpool")
@FunctionalInterface
public interface ThreadPool {

    /**
     * 构建队列
     *
     * @param size
     * @param isPriority
     * @return
     */
    BiFunction<Integer, Boolean, BlockingQueue> QUEUE_FUNCTION = (size, isPriority) -> size == 0 ? new SynchronousQueue<>() : (isPriority ?
            (size < 0 ? new PriorityBlockingQueue<>() : new PriorityBlockingQueue<>(size)) :
            (size < 0 ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(size)));

    /**
     * 构建线程池
     *
     * @param url           URL
     * @param threadFactory 线程工厂类
     * @return
     */
    default ThreadPoolExecutor get(final URL url, final ThreadFactory threadFactory) {
        return get(url, threadFactory, o -> QUEUE_FUNCTION.apply(url.getInteger(QUEUES_OPTION), !url.getString(QUEUE_TYPE_OPTION).equals(QUEUE_TYPE_OPTION.getName())));
    }

    /**
     * 构建线程池
     *
     * @param url           URL
     * @param threadFactory 线程工厂类
     * @param function      队列
     * @return
     */
    ThreadPoolExecutor get(final URL url, final ThreadFactory threadFactory, final Function<URL, BlockingQueue> function);

}
