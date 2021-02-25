package io.joyrpc.thread.adaptive;

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

import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.OverloadException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.function.Function;

import static io.joyrpc.constants.Constants.*;


/**
 * 自适应线程池
 */
@Extension(value = "adaptive")
public class AdaptiveThreadPool implements ThreadPool {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveThreadPool.class);

    @Override
    public ExecutorService get(final URL url, final ThreadFactory threadFactory, final Function<URL, BlockingQueue> function) {
        Integer maxSize = url.getPositiveInt(MAX_SIZE_OPTION);
        Integer coreSize = url.getPositive(CORE_SIZE_OPTION.getName(), maxSize);
        Integer keepAliveTime = url.getPositive(KEEP_ALIVE_TIME_OPTION.getName(), (Integer) null);
        if (maxSize == coreSize) {
            keepAliveTime = keepAliveTime == null ? 0 : keepAliveTime;
        } else if (maxSize < coreSize) {
            maxSize = coreSize;
            keepAliveTime = keepAliveTime == null ? 0 : keepAliveTime;
        } else {
            keepAliveTime = keepAliveTime == null ? KEEP_ALIVE_TIME_OPTION.getValue() : keepAliveTime;
        }
        return new ThreadPoolExecutor(coreSize, maxSize, keepAliveTime, TimeUnit.MILLISECONDS,
                function.apply(url),
                threadFactory,
                new RejectedExecutionHandler() {
                    protected int i = 1;

                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        if (i++ % 7 == 0) {
                            i = 1;
                            logger.warn(String.format("Task:%s has been reject for ThreadPool exhausted! pool:%d, active:%d, queue:%d, tasks: %d",
                                    r, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size(), executor.getTaskCount()
                            ));
                        }
                        throw new OverloadException("Biz thread pool of provider has bean exhausted", ExceptionCode.PROVIDER_THREAD_EXHAUSTED, 0, true);
                    }
                });
    }
}
