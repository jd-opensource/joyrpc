# 插件

实现了插件的加载、排序、选择及缓存等逻辑。可以组合多个插件加载器共同加载插件。

默认提供了基于SPI的插件加载器和基于Spring的插件加载器

## 插件体系

### 扩展点

java接口，默认名称是全路径类名，可以通过注解来描述其名称

```java
@Extensible("consumer")
public interface Consumer {
}
```

### 扩展

对扩展点的实现，可以通过注解或实现优先级、类型和原型接口，来提供更丰富的控制

1. 扩展按照优先级升序排序，默认优先级为Short.MAX_VALUE，定义为Ordered.ORDER常量。可以通过@Extension注解来进行设置，或者实现com.jd.laf.extension.Ordered接口

2. 扩展的类型，默认为全路径类名，可以通过@Extension注解来进行设置，或者实现com.jd.laf.extension.Type接口

3. 扩展默认为单例，可以通过@Extension注解来进行设置，或者实现com.jd.laf.extension.Prototype多例接口。Spring容器中的扩展遵循Spring的单例配置

```java
@Extension("myConsumer")
public class MyConsumer implements Consumer, Ordered {

    @Override
    public int order() {
        return Ordered.ORDER;
    }
}
```
MyConsumer扩展实现了Consumer扩展点，并且自定义优先级。

### 自定排序器

扩展默认按照优先级进行升序排序，可以在加载扩展的时候提供自定义排序器。

例如在数据绑定场景，可以实现按照类型的继承关系的排序器，优先查找最接近类型的处理器

```java
public class AscendingComparator implements Comparator<ExtensionMeta> {

    public static final Comparator INSTANCE = new AscendingComparator();

    @Override
    public int compare(ExtensionMeta o1, ExtensionMeta o2) {
        return o1.getOrder() - o2.getOrder();
    }
}
```

### 自定分类器

如果扩展代码不便于修改了，可以在加载扩展的时候提供分类器，自定义扩展的类型

```java
public interface Classify<T, M> {

    /**
     * 获取类型
     *
     * @param obj
     * @return
     */
    M type(T obj);

}
```

### 条件注解

```java
@Extension("myConsumer1")
@ConditionalOnJava("1.6")
@ConditionalOnClass("xxx.ddf123.df")
public class MyConsumer2 implements Consumer,Ordered {

    @Override
    public int order() {
        return -1;
    }
}

```

### 扩展加载器

通过扩展加载器来加载插件，默认提供基于SPI的插件加载。

```java
public interface ExtensionLoader {

    /**
     * 加载扩展点
     *
     * @param extensible 可扩展的接口
     * @return 扩展点列表
     */
    <T> Collection<Plugin<T>> load(Class<T> extensible);

    /**
     * 包装器
     */
    class Wrapper implements ExtensionLoader {
        protected List<ExtensionLoader> loaders = new LinkedList<ExtensionLoader>();

        public Wrapper(ExtensionLoader... loaders) {
            if (loaders != null) {
                for (ExtensionLoader loader : loaders) {
                    if (loader instanceof Wrapper) {
                        this.loaders.addAll(((Wrapper) loader).loaders);
                    } else {
                        this.loaders.add(loader);
                    }
                }
            }
        }

        @Override
        public <T> Collection<Plugin<T>> load(final Class<T> extensible) {

            List<Plugin<T>> result = new LinkedList<Plugin<T>>();

            if (loaders != null) {
                for (ExtensionLoader loader : loaders) {
                    Collection<Plugin<T>> plugins = loader.load(extensible);
                    if (plugins != null) {
                        result.addAll(plugins);
                    }
                }
            }

            return result;
        }
    }

}
```

基于Spring容器的插件加载器

```java
public class SpringLoader implements ExtensionLoader, PriorityOrdered, ApplicationContextAware {

    protected ApplicationContext context;

    protected Instantiation instance;

    @Override
    public <T> Collection<Plugin<T>> load(final Class<T> extensible) {
        if (extensible == null) {
            return null;
        }
        List<Plugin<T>> result = new LinkedList<Plugin<T>>();
        String[] names = context.getBeanNamesForType(extensible);
        if (names != null) {
            for (String name : names) {
                T plugin = context.getBean(name, extensible);
                result.add(new Plugin<T>(new Name(plugin.getClass(), name), instance, context.isSingleton(name), plugin));
            }
        }
        return result;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        this.context = context;
        this.instance = new Instantiation() {
            @Override
            public <T, M> T newInstance(final Name<T, M> name) {
                try {
                    return context.getBean(name.getName().toString(), name.getClazz());
                } catch (BeansException e) {
                    return null;
                }
            }
        };
        ExtensionManager.wrap(this);
    }
}
```

实例化接口

从Spring加载和从SPI加载的扩展，其实例化对象方法不一样

```java
public interface Instantiation {

    /**
     * 构建实例
     *
     * @param name 实例名称
     * @param <T>
     * @return
     */
    <T, M> T newInstance(Name<T, M> name);

    class ClazzInstance implements Instantiation {

        public static final Instantiation INSTANCE = new ClazzInstance();

        @Override
        public <T, M> T newInstance(final Name<T, M> name) {
            try {
                return name == null ? null : name.getClazz().newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

}
```

### 扩展点接口

对扩展点的操作接口，可以获取优先级最高的扩展，或者获取指定名称的扩展，并且可以正向或反向遍历扩展。同时也可以获取扩展的元数据信息

```java
public interface ExtensionPoint<T, M> {

    /**
     * 按照名称获取指定扩展实现，如果是字符串名称，可以在字符串后面加上"@供应商"来获取指定供应商提供的插件
     *
     * @param name
     * @return
     */
    T get(final M name);

    /**
     * 选择一个实现
     *
     * @return
     */
    T get();

    /**
     * 获取扩展实现列表
     *
     * @return
     */
    Iterable<T> extensions();

    /**
     * 反序获取扩展实现列表
     *
     * @return
     */
    Iterable<T> reverse();

    /**
     * 扩展元数据迭代
     *
     * @return
     */
    Iterable<ExtensionMeta<T, M>> metas();

    /**
     * 扩展元数据迭代
     *
     * @param name 名称
     * @return
     */
    Iterable<ExtensionMeta<T, M>> metas(M name);

    /**
     * 获取扩展元数据
     *
     * @param name 名称
     * @return
     */
    ExtensionMeta<T, M> meta(M name);

    /**
     * 扩展点名称
     *
     * @return
     */
    Name<T, String> getName();
}
```

为了便于Spring的集成，需要延迟加载扩展点，提供了扩展点延迟加载的实现
```java
public class ExtensionPointLazy<T, M> implements ExtensionPoint<T, M> {

    protected ExtensionPoint<T, M> delegate;

    protected final Class<T> extensible;
    protected final ExtensionLoader loader;
    protected final Comparator<ExtensionMeta<T, M>> comparator;
    protected final Classify<T, M> classify;

    public ExtensionPointLazy(Class<T> extensible) {
        this(extensible, null, null, null);
    }

    public ExtensionPointLazy(Class<T> extensible, Comparator<ExtensionMeta<T, M>> comparator) {
        this(extensible, null, comparator, null);
    }

    public ExtensionPointLazy(Class<T> extensible, Classify<T, M> classify) {
        this(extensible, null, null, classify);
    }

    public ExtensionPointLazy(Class<T> extensible, ExtensionLoader loader, Comparator<ExtensionMeta<T, M>> comparator,
                              Classify<T, M> classify) {
        this.extensible = extensible;
        this.loader = loader;
        this.comparator = comparator;
        this.classify = classify;
    }

    protected ExtensionPoint<T, M> getDelegate() {
        if (delegate == null) {
            synchronized (extensible) {
                if (delegate == null) {
                    delegate = ExtensionManager.getOrLoadSpi(extensible, loader, comparator, classify);
                }
            }
        }
        return delegate;
    }

    @Override
    public T get(M name) {
        return getDelegate().get(name);
    }

    @Override
    public T get() {
        return getDelegate().get();
    }

    @Override
    public Iterable<ExtensionMeta<T, M>> metas() {
        return getDelegate().metas();
    }

    @Override
    public Iterable<ExtensionMeta<T, M>> metas(M name) {
        return getDelegate().metas(name);
    }

    @Override
    public ExtensionMeta<T, M> meta(final M name) {
        return getDelegate().meta(name);
    }

    @Override
    public Iterable<T> extensions() {
        return getDelegate().extensions();
    }

    @Override
    public Iterable<T> reverse() {
        return getDelegate().reverse();
    }

    @Override
    public Name<T, String> getName() {
        return getDelegate().getName();
    }
}
```

### 选择器

根据条件选择插件，或对选择的插件进行调用返回结果值。

默认提供了单条匹配，多条匹配和单条转换的选择器基类。并且提供了缓存选择器包装器，对结果进行缓存，加快性能。

```java
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
```

### 扩展点选择器

组合扩展点和选择器，方便调用

```java
public class ExtensionSelector<T, M, C, K> {

    ExtensionPoint<T, M> extensionPoint;

    Selector<T, M, C, K> selector;

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
```


## 使用说明

### 包依赖

#### 非Spring环境

支持基于SPI的插件加载

```xml
<dependency>
    <groupId>com.jd.laf</groupId>
    <artifactId>laf-extension-core</artifactId>
    <version>${laf-extension.version}</version>
</dependency>
```

#### Spring环境

支持基于SPI和Spring的插件加载，需要配置Spring插件加载器

```xml
<dependency>
    <groupId>com.jd.laf</groupId>
    <artifactId>laf-extension-spring</artifactId>
    <version>${laf-extension.version}</version>
</dependency>
```

在Spring的配置文件中配置SpringLoader

```xml
<bean class="com.jd.laf.extension.spring.SpringLoader"/>
```

#### Springboot环境

支持基于SPI和Spring的插件加载，自动注册Spring插件加载器

```xml
<dependency>
    <groupId>com.jd.laf</groupId>
    <artifactId>laf-extension-springboot-starter</artifactId>
    <version>${laf-extension.version}</version>
</dependency>
```

### 实现扩展

可以基于如下方法实现

1). 基于java SPI进行实现，并配置到相关的文件里面。例如JsonProvider的扩展配置如下

src/main/resources/META-INF/services/com.jd.laf.binding.marshaller.JsonProvider

该文件内容为各个扩展实现的全路径类名

```
com.jd.laf.web.vertx.marshaller.JacksonProvider
```

2). 基于Spring注册相关的实现

### 定义扩展常量

通常建议在工程包的根路径下定义Plugin接口类，在其中定义扩展常量，其它地方引用来获取扩展

```java
package com.jd.laf.web.vertx;

import com.jd.laf.extension.ExtensionMeta;
import com.jd.laf.extension.ExtensionPoint;
import com.jd.laf.extension.ExtensionPointLazy;
import com.jd.laf.extension.ExtensionSelector;
import com.jd.laf.extension.Selector.CacheSelector;
import com.jd.laf.extension.Selector.MatchSelector;
import com.jd.laf.web.vertx.lifecycle.Registrar;
import com.jd.laf.web.vertx.message.CustomCodec;
import com.jd.laf.web.vertx.pool.PoolFactory;
import com.jd.laf.web.vertx.render.Render;
import com.jd.laf.web.vertx.response.ErrorSupplier;
import com.jd.laf.web.vertx.service.Daemon;

import java.util.Comparator;

public interface Plugin {

    //消息插件
    ExtensionPoint<MessageHandler, String> MESSAGE = new ExtensionPointLazy(MessageHandler.class);
    //异常处理插件
    ExtensionPoint<ErrorHandler, String> ERROR = new ExtensionPointLazy(ErrorHandler.class);
    //路由插件
    ExtensionPoint<RoutingHandler, String> ROUTING = new ExtensionPointLazy(RoutingHandler.class);
    //命令插件
    ExtensionPoint<Command, String> COMMAND = new ExtensionPointLazy(Command.class);
    //注册器插件
    ExtensionPoint<Registrar, String> REGISTRAR = new ExtensionPointLazy(Registrar.class);
    //渲染插件
    ExtensionPoint<Render, String> RENDER = new ExtensionPointLazy(Render.class);
    //对象工厂插件
    ExtensionPoint<PoolFactory, String> POOL = new ExtensionPointLazy(PoolFactory.class);
    //模板插件
    ExtensionPoint<TemplateProvider, String> TEMPLATE = new ExtensionPointLazy(TemplateProvider.class);
    //守护服务插件
    ExtensionPoint<Daemon, String> DAEMON = new ExtensionPointLazy(Daemon.class);
    //编解码插件
    ExtensionPoint<CustomCodec, String> CODEC = new ExtensionPointLazy(CustomCodec.class);
    //异常处理插件
    ExtensionSelector<ErrorSupplier, Class<?>, Class<?>, ErrorSupplier> THROWABLE = new ExtensionSelector<>(
            new ExtensionPointLazy(ErrorSupplier.class, (Comparator<ExtensionMeta<ErrorSupplier, Class<?>>>) (o1, o2) -> {
                ErrorSupplier e1 = o1.getTarget();
                ErrorSupplier e2 = o2.getTarget();
                if (e1.type() == e2.type()) {
                    return 0;
                } else if (e1.type().isAssignableFrom(e2.type())) {
                    return 1;
                } else if (e2.type().isAssignableFrom(e1.type())) {
                    return -1;
                }
                return 0;
            }),
            new CacheSelector<>(new MatchSelector<ErrorSupplier, Class<?>, Class<?>>() {

                @Override
                protected boolean match(final ErrorSupplier target, final Class<?> condition) {
                    return target.type().isAssignableFrom(condition);
                }
            })
    );
}
```

调用扩展的代码如下
```
command = COMMAND.get(name);
```
