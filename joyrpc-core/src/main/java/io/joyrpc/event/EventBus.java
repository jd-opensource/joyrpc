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

import io.joyrpc.extension.Extensible;

/**
 * 事件总线
 *
 * @date: 2019/3/12
 */
@Extensible("eventBus")
public interface EventBus {

    /**
     * 创建消息发布者
     *
     * @param group  分组，相同分组统一的派发线程
     * @param name   名称
     * @param config 配置
     * @param <E>
     * @return
     */
    <E extends Event> Publisher<E> getPublisher(String group, String name, PublisherConfig config);

    /**
     * 创建消息发布者
     *
     * @param group 分组，相同分组统一的派发线程
     * @param name  名称
     * @param <E>
     * @return
     */
    default <E extends Event> Publisher<E> getPublisher(final String group, final String name) {
        return getPublisher(group, name, null);
    }

}
