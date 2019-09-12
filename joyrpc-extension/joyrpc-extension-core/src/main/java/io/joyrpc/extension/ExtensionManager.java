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

import io.joyrpc.extension.listener.ExtensionListener;
import io.joyrpc.extension.listener.LoaderEvent;
import io.joyrpc.extension.spi.SpiLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.sort;

/**
 * 扩展点管理
 */
public abstract class ExtensionManager {
    //扩展点快照
    protected static volatile Snapshot SNAPSHOT = new Snapshot(io.joyrpc.extension.spi.SpiLoader.INSTANCE);

    protected static List<ExtensionListener> LISTENERS = new CopyOnWriteArrayList<ExtensionListener>();

    /**
     * 获取扩展实现
     *
     * @param type 类型
     * @param name 扩展名称
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> T getExtension(final String type, final M name) {
        return SNAPSHOT.getExtension(type, name);
    }

    /**
     * 获取扩展实现
     *
     * @param extensible 扩展点类
     * @param name       扩展名称
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> T getExtension(final Class<T> extensible, final M name) {
        ExtensionPoint spi = SNAPSHOT.getExtensionPoint(extensible);
        return spi == null ? null : (T) spi.get(name);
    }

    /**
     * 获取或加载扩展实现
     *
     * @param extensible 扩展点类
     * @param name       扩展名称
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> T getOrLoadExtension(final Class<T> extensible, final M name) {
        ExtensionPoint spi = SNAPSHOT.getOrLoadExtensionPoint(extensible, null, ExtensionMeta.AscendingComparator.INSTANCE, null);
        return spi == null ? null : (T) spi.get(name);
    }

    /**
     * 获取或加载扩展实现
     *
     * @param extensible 扩展点类
     * @param <T>        扩展实现
     * @return
     */
    public static <T> T getOrLoadExtension(final Class<T> extensible) {
        ExtensionPoint spi = SNAPSHOT.getOrLoadExtensionPoint(extensible, null, ExtensionMeta.AscendingComparator.INSTANCE, null);
        return spi == null ? null : (T) spi.get();
    }

    /**
     * 获取扩展实现
     *
     * @param extensible 扩展点类型
     * @param <T>
     * @return
     */
    public static <T> Iterable<T> getExtensions(final Class<T> extensible) {
        ExtensionPoint spi = SNAPSHOT.getExtensionPoint(extensible);
        return spi == null ? null : spi.extensions();
    }

    /**
     * 获取扩展实现
     *
     * @param extensible 扩展点类型
     * @param <T>
     * @return
     */
    public static <T> Iterable<T> getOrLoadExtensions(final Class<T> extensible) {
        ExtensionPoint spi = SNAPSHOT.getOrLoadExtensionPoint(extensible, null, ExtensionMeta.AscendingComparator.INSTANCE, null);
        return spi == null ? null : spi.extensions();
    }

    /**
     * 获取扩展点实现
     *
     * @param type 类型
     * @param <T>
     * @return
     */
    public static <T> Iterable<T> get(final String type) {
        return SNAPSHOT.getExtensions(type);
    }

    /**
     * 获取扩展点
     *
     * @param extensible
     * @return
     */
    public static <T, M> ExtensionPoint<T, M> getOrLoadExtensionPoint(final Class<T> extensible) {
        return SNAPSHOT.getOrLoadExtensionPoint(extensible, null, ExtensionMeta.AscendingComparator.INSTANCE, null);
    }

    /**
     * 获取扩展点
     *
     * @param extensible
     * @param loader
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> ExtensionPoint<T, M> getOrLoadExtensionPoint(final Class<T> extensible, final ExtensionLoader loader) {
        return SNAPSHOT.getOrLoadExtensionPoint(extensible, loader, ExtensionMeta.AscendingComparator.INSTANCE, null);
    }

    /**
     * 获取扩展点
     *
     * @param extensible
     * @param comparator
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> ExtensionPoint<T, M> getOrLoadExtensionPoint(final Class<T> extensible,
                                                                      final Comparator<ExtensionMeta<T, M>> comparator) {
        return SNAPSHOT.getOrLoadExtensionPoint(extensible, null, comparator, null);
    }

    /**
     * 获取扩展点
     *
     * @param extensible
     * @param loader
     * @param comparator
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> ExtensionPoint<T, M> getOrLoadExtensionPoint(final Class<T> extensible,
                                                                      final ExtensionLoader loader,
                                                                      final Comparator<ExtensionMeta<T, M>> comparator) {
        return SNAPSHOT.getOrLoadExtensionPoint(extensible, loader, comparator, null);
    }

    /**
     * 获取扩展点
     *
     * @param extensible
     * @param loader
     * @param comparator
     * @param classify
     * @param <T>
     * @param <M>
     * @return
     */
    public static <T, M> ExtensionPoint<T, M> getOrLoadExtensionPoint(final Class<T> extensible,
                                                                      final ExtensionLoader loader,
                                                                      final Comparator<ExtensionMeta<T, M>> comparator,
                                                                      final Classify<T, M> classify) {
        return SNAPSHOT.getOrLoadExtensionPoint(extensible, loader, comparator, classify);
    }

    /**
     * 加载扩展点
     *
     * @param extensibles
     */
    public static void loadExtension(final Collection<Class<?>> extensibles) {
        SNAPSHOT.loadExtension(extensibles);
    }

    /**
     * 加载扩展点
     *
     * @param extensibles
     * @param loader
     */
    public static void loadExtension(final Collection<Class<?>> extensibles, final ExtensionLoader loader) {
        SNAPSHOT.loadExtension(extensibles, loader);
    }

    /**
     * 加载扩展点
     *
     * @param scanner
     */
    public static void loadExtension(final ExtensionScanner scanner) {
        if (scanner != null) {
            SNAPSHOT.loadExtension(scanner.scan());
        }
    }

    /**
     * 初始化扫描插件并加载
     *
     * @param scanner
     * @param loader
     */
    public static void loadExtension(final ExtensionScanner scanner, final ExtensionLoader loader) {
        if (scanner != null) {
            SNAPSHOT.loadExtension(scanner.scan(), loader);
        }
    }

    /**
     * 注册插件加载器
     *
     * @param loader
     */
    public synchronized static void register(final ExtensionLoader loader) {
        apply(SNAPSHOT.register(loader));
    }

    /**
     * 比较
     *
     * @param snapshot
     */
    protected static void apply(final Snapshot snapshot) {
        boolean flag = SNAPSHOT == snapshot;
        if (!flag) {
            SNAPSHOT = snapshot;
            for (ExtensionListener listener : LISTENERS) {
                listener.onEvent(new LoaderEvent(SNAPSHOT));
            }
        }
    }

    /**
     * 注销插件加载器
     *
     * @param loader
     */
    public synchronized static void deregister(final ExtensionLoader loader) {
        apply(SNAPSHOT.deregister(loader));
    }

    /**
     * 添加监听器
     *
     * @param listener
     * @return
     */
    public static boolean addListener(final ExtensionListener listener) {
        if (listener == null) {
            return false;
        }
        return LISTENERS.add(listener);
    }

    /**
     * 指定加载器的扩展点快照
     */
    protected static class Snapshot {

        // 扩展点名称
        protected ConcurrentMap<String, ExtensionSpi> names = new ConcurrentHashMap<String, ExtensionSpi>();
        // 扩展点
        protected ConcurrentMap<Class, ExtensionSpi> extensions = new ConcurrentHashMap<Class, ExtensionSpi>();
        // 加载器
        protected ExtensionLoader loader;

        public Snapshot() {
            this(SpiLoader.INSTANCE);
        }

        public Snapshot(ExtensionLoader loader) {
            this.loader = loader == null ? SpiLoader.INSTANCE : loader;
        }

        protected void addTo(final Set<ExtensionLoader> loaders, final ExtensionLoader loader) {
            if (loader == null) {
                return;
            }
            if (loader instanceof ExtensionLoader.Wrapper) {
                for (ExtensionLoader l : ((ExtensionLoader.Wrapper) loader).loaders) {
                    addTo(loaders, l);
                }
            } else {
                loaders.add(loader);
            }
        }

        /**
         * 注册插件加载器
         *
         * @param loader
         */
        public <T, M> Snapshot register(final ExtensionLoader loader) {
            if (loader == null) {
                return this;
            } else if (loader == this.loader) {
                return this;
            }
            //旧插件
            Set<ExtensionLoader> loaders = new LinkedHashSet<ExtensionLoader>();
            addTo(loaders, this.loader);
            //新插件
            Set<ExtensionLoader> newLoaders = new LinkedHashSet<ExtensionLoader>();
            addTo(newLoaders, loader);
            if (!loaders.addAll(newLoaders)) {
                //已经存在
                return this;
            }
            //新插件加载器
            ExtensionLoader wrapper = new ExtensionLoader.Wrapper(newLoaders);
            //构造数据快照
            Snapshot result = new Snapshot(new ExtensionLoader.Wrapper(loaders));
            ExtensionSpi<T, M> spi;
            Name<T, String> name;
            List<ExtensionMeta<T, M>> metas;
            //遍历已经加载的插件，追加新的插件
            for (Map.Entry<Class, ExtensionSpi> entry : extensions.entrySet()) {
                spi = entry.getValue();
                name = spi.name;
                metas = new LinkedList<ExtensionMeta<T, M>>();
                //原有的插件
                metas.addAll(spi.metas);
                //加载新插件
                load(name.getClazz(), name, wrapper, spi.classify, metas);
                //排序
                sort(metas, spi.comparator);
                //构造新的扩展点
                result.add(new ExtensionSpi(name, metas, spi.comparator, spi.classify));
            }
            return result;
        }

        /**
         * 注销插件加载器
         *
         * @param loader
         */
        public <T, M> Snapshot deregister(final ExtensionLoader loader) {
            if (loader == null) {
                return this;
            } else if (loader == this.loader) {
                return new Snapshot();
            }
            //旧插件
            Set<ExtensionLoader> loaders = new LinkedHashSet<ExtensionLoader>();
            addTo(loaders, this.loader);
            //删除的插件
            Set<ExtensionLoader> excludes = new LinkedHashSet<ExtensionLoader>();
            addTo(excludes, loader);
            if (!loaders.removeAll(excludes)) {
                //待删除的插件不在旧的插件里面
                return this;
            } else if (loaders.isEmpty()) {
                return new Snapshot();
            }

            //构造数据快照
            Snapshot result = new Snapshot(loaders.size() == 1 ? loaders.iterator().next() : new ExtensionLoader.Wrapper(loaders));
            ExtensionSpi<T, M> spi;
            Name<T, String> name;
            List<ExtensionMeta<T, M>> metas;
            //遍历已经加载的插件，删除注销的插件加载器所加载的插件
            for (Map.Entry<Class, ExtensionSpi> entry : extensions.entrySet()) {
                spi = entry.getValue();
                name = spi.name;
                metas = new LinkedList<ExtensionMeta<T, M>>();
                for (ExtensionMeta<T, M> meta : spi.metas) {
                    if (!excludes.contains(meta.loader)) {
                        metas.add(meta);
                    }
                }
                //构造新的扩展点
                result.add(new ExtensionSpi(name, metas, spi.comparator, spi.classify));
            }
            return result;

        }

        protected <T, M> ExtensionSpi<T, M> add(final ExtensionSpi<T, M> ExtensionPoint) {
            if (ExtensionPoint != null) {
                Name<T, String> name = ExtensionPoint.getName();
                //防止并发
                ExtensionSpi<T, M> exists = extensions.putIfAbsent(name.getClazz(), ExtensionPoint);
                if (exists == null) {
                    if (name.getName() != null) {
                        names.put(name.getName(), ExtensionPoint);
                    }
                    return ExtensionPoint;
                }
                return exists;
            }
            return null;
        }

        /**
         * 获取扩展点实现
         *
         * @param type 类型
         * @param <T>
         * @return
         */
        public <T, M> Iterable<T> getExtensions(final String type) {
            ExtensionPoint<T, M> spi = names.get(type);
            return spi == null ? null : spi.extensions();
        }

        /**
         * 获取扩展实现
         *
         * @param type 类型
         * @param name 扩展名称
         * @param <T>
         * @return
         */
        public <T, M> T getExtension(final String type, final M name) {
            ExtensionPoint<T, M> spi = names.get(type);
            return spi == null ? null : (T) spi.get(name);
        }

        /**
         * 获取插件接口
         *
         * @param extensible
         * @return
         */
        public <T, M> ExtensionPoint<T, M> getExtensionPoint(final Class<T> extensible) {
            return extensions.get(extensible);
        }

        /**
         * 获取或加载扩展点
         *
         * @param extensible
         * @param loader
         * @param comparator
         * @param classify
         * @param <T>
         * @param <M>
         * @return
         */
        public <T, M> ExtensionPoint getOrLoadExtensionPoint(final Class<T> extensible,
                                                             final ExtensionLoader loader,
                                                             final Comparator<ExtensionMeta<T, M>> comparator,
                                                             final Classify<T, M> classify) {
            if (extensible == null) {
                return null;
            }
            //判断是否重复添加
            ExtensionPoint<T, M> result = getExtensionPoint(extensible);
            if (result == null) {
                //获取扩展点注解
                Extensible annotation = extensible.getAnnotation(Extensible.class);
                //构造扩展点名称
                Name extensibleName = new Name(extensible, annotation != null && annotation.value() != null
                        && !annotation.value().isEmpty() ? annotation.value() : extensible.getName());
                //加载插件
                List<ExtensionMeta<T, M>> metas = new LinkedList<ExtensionMeta<T, M>>();
                load(extensible, extensibleName, loader, classify, metas);
                //排序
                Comparator c = comparator == null ? ExtensionMeta.AscendingComparator.INSTANCE : comparator;
                sort(metas, c);

                result = add(new ExtensionSpi(extensibleName, metas, c, classify));
            }
            return result;
        }

        /**
         * 加载扩展点
         *
         * @param extensible
         * @param extensibleName
         * @param loader
         * @param classify
         * @param metas
         * @param <T>
         * @param <M>
         */
        protected <T, M> void load(final Class<T> extensible, final Name extensibleName, final ExtensionLoader loader,
                                   final Classify<T, M> classify, final List<ExtensionMeta<T, M>> metas) {
            //加载插件
            Collection<Plugin<T>> plugins = loader == null ? this.loader.load(extensible) : loader.load(extensible);
            for (Plugin<T> plugin : plugins) {
                Class<?> pluginClass = plugin.name.getClazz();
                Extension extension = pluginClass.getAnnotation(Extension.class);
                ExtensionMeta<T, M> meta = new ExtensionMeta<T, M>();
                //记录加载器信息，便于卸载加载器
                meta.setLoader(plugin.loader);
                meta.setExtensible(extensibleName);
                meta.setName(plugin.name);
                meta.setProvider(extension != null && !extension.provider().isEmpty() ? extension.provider() : pluginClass.getName());
                meta.setInstantiation(plugin.instantiation == null ? Instantiation.ClazzInstance.INSTANCE : plugin.instantiation);
                meta.setTarget(plugin.target);
                meta.setSingleton(plugin.isSingleton() != null ? plugin.isSingleton() :
                        (Prototype.class.isAssignableFrom(pluginClass) ? false :
                                (extension == null ? true : extension.singleton())));
                //获取插件，不存在则创建
                T target = meta.getTarget();
                meta.setExtension(new Name(pluginClass, classify != null ? classify.type(target) :
                        (Type.class.isAssignableFrom(pluginClass) ? ((Type) target).type() :
                                (extension != null && extension.value() != null && !extension.value().isEmpty() ? extension.value() :
                                        pluginClass.getName()))));
                meta.setOrder(Ordered.class.isAssignableFrom(pluginClass) ? ((Ordered) target).order() :
                        (extension == null ? Ordered.ORDER : extension.order()));
                //判断是否禁用了该插件
                if (!Disable.isDisable(meta)) {
                    metas.add(meta);
                }

            }
        }

        /**
         * 加载扩展点集合
         *
         * @param extensibles
         */
        public void loadExtension(final Collection<Class<?>> extensibles) {
            loadExtension(extensibles, loader);
        }

        /**
         * 加载扩展点集合
         *
         * @param extensibles
         * @param loader
         */
        public void loadExtension(final Collection<Class<?>> extensibles, final ExtensionLoader loader) {
            if (extensibles != null) {
                ExtensionLoader extensionLoader = loader == null ? this.loader : loader;
                for (Class extensible : extensibles) {
                    getOrLoadExtensionPoint(extensible, extensionLoader, null, null);
                }
            }
        }
    }

}
