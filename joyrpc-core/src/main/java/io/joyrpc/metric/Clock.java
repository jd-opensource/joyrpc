package io.joyrpc.metric;

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

import java.util.concurrent.TimeUnit;

/**
 * 时钟
 *
 * @date 2019年2月19日 下午4:36:58
 */
public interface Clock {

    Clock NANO = new NanoClock();
    Clock MILLI = new MilliClock();

    /**
     * 当前时间戳
     *
     * @return 当前时间
     */
    long getTime();

    /**
     * 单位
     *
     * @return 时间单位
     */
    TimeUnit getTimeUnit();


    /**
     * 纳秒时钟
     */
    class NanoClock implements Clock {

        @Override
        public long getTime() {
            return System.nanoTime();
        }

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.NANOSECONDS;
        }

    }

    /**
     * 毫秒时钟
     */
    class MilliClock implements Clock {


        @Override
        public long getTime() {
            return System.currentTimeMillis();
        }

        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.MILLISECONDS;
        }

    }

}
