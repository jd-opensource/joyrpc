package io.joyrpc.permission;

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

import java.util.Collection;

/**
 * 白名单
 */
public interface WhiteList<T> {

    /**
     * 判断是否在白名单中
     *
     * @param target
     * @return
     */
    boolean isWhite(T target);

    /**
     * 重置动态白名单
     *
     * @param targets
     */
    void updateWhite(Collection<T> targets);

    /**
     * 感知白名单
     */
    @FunctionalInterface
    interface WhiteListAware {

        /**
         * 重置远程白名单
         *
         * @param blackList 白名单配置信息
         */
        void updateWhite(String blackList);

    }
}
