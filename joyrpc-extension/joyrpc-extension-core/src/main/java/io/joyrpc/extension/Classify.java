package io.joyrpc.extension;

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

/**
 * 分类算法
 */
public interface Classify<T, M> {

    /**
     * 获取类型
     *
     * @param obj 扩展对象
     * @return 类型
     */
    M type(T obj);

    /**
     * 获取类型
     *
     * @param meta 扩展元数据
     * @return 类型
     */
    //TODO 是否需要
    default M type(ExtensionMeta<T, M> meta) {
        T target = meta.getTarget();
        return target == null ? null : type(target);
    }

}
