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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 扩展选择器
 *
 * @param <T>
 * @param <M>
 */
public interface Selector<T, M, C, K> {

    /**
     * 选择扩展
     *
     * @param extensions 排序的扩展点集合
     * @param condition  条件
     * @return
     */
    K select(ExtensionPoint<T, M> extensions, C condition);

    /**
     * 匹配选择器
     *
     * @param <T>
     * @param <M>
     */
    abstract class MatchSelector<T, M, C> implements Selector<T, M, C, T> {

        @Override
        public T select(final ExtensionPoint<T, M> extensions, final C condition) {
            T target;
            for (ExtensionMeta<T, M> meta : extensions.metas()) {
                target = meta.getTarget();
                if (target != null && match(target, condition)) {
                    return target;
                }
            }
            return null;
        }

        /**
         * 判断是否匹配
         *
         * @param target
         * @param condition
         * @return
         */
        protected abstract boolean match(T target, C condition);
    }

    /**
     * 列表选择器
     *
     * @param <T>
     * @param <M>
     */
    abstract class ListSelector<T, M, C> implements Selector<T, M, C, List<T>> {

        @Override
        public List<T> select(final ExtensionPoint<T, M> extensions, final C condition) {
            List<T> result = new LinkedList<T>();
            T target;
            for (ExtensionMeta<T, M> meta : extensions.metas()) {
                target = meta.getTarget();
                if (target != null && match(target, condition)) {
                    result.add(target);
                }
            }
            return result;
        }

        /**
         * 判断是否匹配
         *
         * @param target
         * @param condition
         * @return
         */
        protected abstract boolean match(T target, C condition);
    }

    /**
     * 转换选择器
     *
     * @param <T>
     * @param <M>
     */
    abstract class ConverterSelector<T, M, C, K> implements Selector<T, M, C, K> {

        @Override
        public K select(final ExtensionPoint<T, M> extensions, final C condition) {
            T target;
            K result;
            for (ExtensionMeta<T, M> meta : extensions.metas()) {
                target = meta.getTarget();
                if (target != null) {
                    result = convert(target, condition);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }

        /**
         * 进行转换
         *
         * @param target
         * @param condition
         * @return
         */
        protected abstract K convert(T target, C condition);
    }

    /**
     * 缓存选择器，对选择的结果进行缓存
     *
     * @param <T>
     * @param <M>
     * @param <C>
     * @param <K>
     */
    class CacheSelector<T, M, C, K> implements Selector<T, M, C, K> {

        protected ConcurrentMap<C, Option<K>> cache = new ConcurrentHashMap<C, Option<K>>();

        protected Selector<T, M, C, K> delegate;

        public CacheSelector(Selector<T, M, C, K> delegate) {
            this.delegate = delegate;
        }

        @Override
        public K select(final ExtensionPoint<T, M> extensions, final C condition) {
            if (condition == null) {
                return null;
            }
            //根据条件直接返回固定常量
            K result = before(condition);
            if (result != null) {
                return result;
            }
            //从缓存中获取
            Option<K> option = cache.get(condition);
            if (option == null) {
                // 选择插件
                result = delegate.select(extensions, condition);
                if (result == null) {
                    //没有找到
                    result = fail(condition);
                }
                option = new Option<K>(result);
                Option<K> exists = cache.putIfAbsent(condition, option);
                if (exists != null) {
                    option = exists;
                }
            }
            return option.get();
        }

        /**
         * 缓存获取之前，便于根据条件直接返回固定常量
         *
         * @param condition 条件
         * @return
         */
        protected K before(final C condition) {
            return null;
        }

        /**
         * 失败，没有选择到合适的插件进行处理
         *
         * @param condition 条件
         * @return
         */
        protected K fail(final C condition) {
            return null;
        }
    }

}
