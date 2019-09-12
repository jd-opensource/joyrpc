package io.joyrpc.event;

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

import java.util.function.Predicate;

/**
 * @date: 2019/3/12
 */
@FunctionalInterface
public interface EventHandler<E extends Event> {

    /**
     * 处理事件，要求事件不能阻塞
     *
     * @param event
     */
    void handle(E event);

    /**
     * 包装器
     *
     * @param predicate
     * @return
     */
    default EventHandler<E> wrap(final Predicate<E> predicate) {
        return e -> {
            if (predicate == null || predicate.test(e)) {
                handle(e);
            }
        };
    }
}
