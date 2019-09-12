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
 * 扩展点选择器
 *
 * @param <T>
 * @param <M>
 * @param <C>
 * @param <K>
 */
public class ExtensionSelector<T, M, C, K> {
    /**
     * 扩展点
     */
    protected ExtensionPoint<T, M> extensionPoint;
    /**
     * 选择器
     */
    protected Selector<T, M, C, K> selector;

    /**
     * 构造函数
     *
     * @param extensionPoint
     * @param selector
     */
    public ExtensionSelector(ExtensionPoint<T, M> extensionPoint, Selector<T, M, C, K> selector) {
        this.extensionPoint = extensionPoint;
        this.selector = selector;
    }

    /**
     * 选择
     *
     * @param condition 条件
     * @return
     */
    public K select(final C condition) {
        return selector.select(extensionPoint, condition);
    }

}
