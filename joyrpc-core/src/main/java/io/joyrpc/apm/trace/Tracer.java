package io.joyrpc.apm.trace;

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

import java.util.Map;

/**
 * 跟踪会话
 */
public interface Tracer {
    /**
     * 主线程开始
     *
     * @param name      跟踪名称
     * @param component 组件名称
     * @param tags      标签
     */
    void begin(String name, String component, Map<String, String> tags);

    /**
     * 主线程做快照
     */
    default void snapshot(){

    }

    /**
     * 主线程结束
     */
    default void prepare(){

    }

    /**
     * 异步线程恢复
     */
    default void restore(){

    }

    /**
     * 异步线程调用结束
     *
     * @param throwable 异常
     */
    void end(Throwable throwable);
}
