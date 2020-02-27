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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 指定接口的扩展点
 *
 * @param <T> 插件类型
 * @param <M> 插件名称
 */
public class ExtensionSpi<T, M> implements ExtensionPoint<T, M> {

    //按照名称分组聚合的扩展元数据
    protected ConcurrentMap<M, List<ExtensionMeta<T, M>>> multiNames;
    //按照"名称@提供者"分组
    protected ConcurrentMap<String, ExtensionMeta<T, M>> providers;
    //按照名称覆盖的扩展元数据
    protected ConcurrentMap<M, ExtensionMeta<T, M>> names;
    //扩展元数据列表
    protected List<ExtensionMeta<T, M>> metas;
    //扩展点名称
    protected Name<T, String> name;
    //缓存默认插件单例实例
    protected T target;
    //比较器
    protected Comparator<ExtensionMeta<?, ?>> comparator;
    //分类器
    protected Classify<T, M> classify;
    //是否都是单例
    protected boolean singleton = true;

    //缓存的插件列表
    protected volatile List<T> extensions;
    //缓存的插件反序遍历
    protected volatile List<T> reverses;

    public ExtensionSpi(final Name<T, String> name, final List<ExtensionMeta<T, M>> metas,
                        final Comparator<ExtensionMeta<?, ?>> comparator, final Classify<T, M> classify) {
        this.name = name;
        this.metas = new LinkedList<>();
        this.names = new ConcurrentHashMap<>(metas.size());
        this.multiNames = new ConcurrentHashMap<>(metas.size());
        this.providers = new ConcurrentHashMap<>(metas.size());
        this.comparator = comparator;
        this.classify = classify;
        for (ExtensionMeta<T, M> meta : metas) {
            add(meta);
        }
    }

    protected void add(final ExtensionMeta<T, M> meta) {
        if (meta == null) {
            return;
        }
        if (!meta.isSingleton()) {
            singleton = false;
        }
        //扩展名称
        M name = meta.getExtension().getName();
        if (name != null) {
            //防止被覆盖
            names.putIfAbsent(name, meta);
            //相同名称，不同供应商的插件集合
            List<ExtensionMeta<T, M>> metas = multiNames.computeIfAbsent(name, t -> new CopyOnWriteArrayList<>());
            metas.add(meta);

            if (name instanceof String && meta.getProvider() != null && !meta.getProvider().isEmpty()) {
                providers.putIfAbsent(name + "@" + meta.getProvider(), meta);
            }
        }

        this.metas.add(meta);
    }

    protected T getObject(final ExtensionMeta<T, M> extension) {
        return extension == null ? null : extension.getTarget();
    }

    @Override
    public T get(final M name) {
        return name == null ? null : getObject(meta(name));
    }

    @Override
    public T get(final M name, final M option) {
        T result = get(name);
        return result != null ? result : get(option);
    }

    @Override
    public T getOrDefault(final M name) {
        T result = get(name);
        return result != null ? result : get();
    }

    @Override
    public T get() {
        if (target == null && !metas.isEmpty()) {
            ExtensionMeta<T, M> meta = metas.get(0);
            if (meta != null) {
                if (meta.isSingleton()) {
                    target = meta.getTarget();
                } else {
                    return meta.getTarget();
                }
            }
        }
        return target;
    }

    @Override
    public Iterable<ExtensionMeta<T, M>> metas() {
        return metas;
    }

    @Override
    public Iterable<ExtensionMeta<T, M>> metas(final M name) {
        return name == null ? null : multiNames.get(name);
    }

    @Override
    public ExtensionMeta<T, M> meta(final M name) {
        ExtensionMeta<T, M> result = null;
        if (name != null) {
            //按照插件名称获取
            result = names.get(name);
            if (result == null) {
                //没有找到，猜测名称里面是有提供商
                if (name instanceof String) {
                    result = providers.get(name);
                    if (result == null) {
                        //供应商也没有找到，则尝试去掉供应商查找
                        String v = (String) name;
                        int pos = v.indexOf('@');
                        if (pos > 0) {
                            result = names.get(name);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int size() {
        return metas.size();
    }

    /**
     * 构造扩展点列表
     *
     * @param predicate
     * @return
     */
    protected List<T> doExtensions(final Predicate<T> predicate) {
        LinkedList<T> result = new LinkedList<T>();
        T object;
        for (ExtensionMeta<T, M> extension : metas) {
            object = getObject(extension);
            if (object != null && (predicate == null || predicate.test(object))) {
                result.add(object);
            }
        }
        return result;
    }

    @Override
    public Iterable<T> extensions() {
        return extensions(null);
    }

    @Override
    public Iterable<T> extensions(final Predicate<T> predicate) {
        if (singleton) {
            //单例可以缓存
            if (extensions == null) {
                synchronized (this) {
                    if (extensions == null) {
                        extensions = doExtensions(null);
                    }
                }
            }
            if (predicate == null) {
                return extensions;
            } else {
                LinkedList<T> result = new LinkedList<T>();
                for (T v : extensions) {
                    if (predicate.test(v)) {
                        result.add(v);
                    }
                }
                return result;
            }
        }
        return doExtensions(predicate);
    }

    /**
     * 反序列表
     *
     * @return
     */
    protected List<T> doReverses() {
        LinkedList<T> result = new LinkedList<T>();
        T object;
        for (ExtensionMeta<T, M> extension : metas) {
            object = getObject(extension);
            if (object != null) {
                result.addFirst(object);
            }
        }
        return result;
    }

    @Override
    public Iterable<T> reverse() {
        if (singleton) {
            //单例可以缓存
            if (reverses == null) {
                synchronized (this) {
                    if (reverses == null) {
                        reverses = doReverses();
                    }
                }
            }
            return reverses;
        }
        return doReverses();
    }

    @Override
    public Name<T, String> getName() {
        return name;
    }

    @Override
    public List<M> names() {
        List<M> result = new ArrayList<M>(metas.size());
        for (ExtensionMeta<T, M> meta : metas) {
            result.add(meta.extension.getName());
        }
        return result;
    }

    @Override
    public List<M> available(final List<M> names) {
        List<M> result = new ArrayList<M>(names == null ? 0 : names.size());
        if (names != null) {
            for (M name : names) {
                if (meta(name) != null) {
                    result.add(name);
                }
            }
        }
        return result;
    }
}
