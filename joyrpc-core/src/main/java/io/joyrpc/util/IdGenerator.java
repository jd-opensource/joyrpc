package io.joyrpc.util;

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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * ID生成器
 */
public interface IdGenerator<M> extends Supplier<M> {

    /**
     * 整数ID生成器
     */
    class IntIdGenerator implements IdGenerator<Integer> {

        protected AtomicInteger id = new AtomicInteger(0);

        @Override
        public Integer get() {
            return id.incrementAndGet();
        }

    }

    /**
     * 长整数ID生成器
     */
    class LongIdGenerator implements IdGenerator<Long> {

        protected AtomicLong id = new AtomicLong(0);

        @Override
        public Long get() {
            return id.incrementAndGet();
        }

    }

}
