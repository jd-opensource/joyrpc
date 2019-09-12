package io.joyrpc.extension.spi;

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

import io.joyrpc.extension.ExtensionLoader;
import io.joyrpc.extension.Name;
import io.joyrpc.extension.Plugin;
import io.joyrpc.extension.condition.Condition;
import io.joyrpc.extension.condition.Conditional;
import io.joyrpc.extension.exception.PluginException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;

/**
 * SPI加载插件
 */
public class SpiLoader implements ExtensionLoader {

    public static final ExtensionLoader INSTANCE = new SpiLoader();

    protected static final String PREFIX = "META-INF/services/";

    @Override
    public <T> Collection<Plugin<T>> load(final Class<T> extensible) {
        if (extensible == null) {
            return null;
        }
        List<Plugin<T>> result = new LinkedList<Plugin<T>>();

        ClassLoader loader = getClassLoader(extensible);
        try {
            //获取插件名称
            Collection<String> classNames = loadPluginName(extensible, loader, getResource(extensible));
            Class<T> tClass;
            //遍历插件
            for (String className : classNames) {
                //加载类，过滤掉不符合条件的
                tClass = loadPluginClass(extensible, loader, className);
                if (tClass != null) {
                    //实例化插件
                    result.add(new Plugin<T>(new Name(tClass), tClass.newInstance(), this));
                }
            }
        } catch (InstantiationException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PluginException(extensible.getName() + ": Error reading configuration file", e);
        }
        return result;
    }

    /**
     * 获取类加载器
     *
     * @param service
     * @return
     */
    protected ClassLoader getClassLoader(final Class service) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = service.getClassLoader();
        }
        return classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    /**
     * 获取资源名称
     *
     * @param service
     * @return
     */
    protected String getResource(final Class service) {
        return PREFIX + service.getName();
    }

    /**
     * 加载插件名称
     *
     * @param service
     * @param loader
     * @param resource
     * @return
     * @throws IOException
     */
    protected Collection<String> loadPluginName(final Class service, final ClassLoader loader, final String resource) throws IOException {
        Set<String> names = new LinkedHashSet<String>();
        Enumeration<URL> configs = loader.getResources(resource);

        URL url;
        BufferedReader reader;
        while ((configs.hasMoreElements())) {
            url = configs.nextElement();
            reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            try {
                loadPluginName(service, reader, names);
            } finally {
                close(reader);
            }
        }
        return names;
    }

    /**
     * 加载插件名称
     *
     * @param service
     * @param reader
     * @param names
     * @throws IOException
     */
    protected void loadPluginName(final Class service, final BufferedReader reader, final Set<String> names) throws IOException {
        String ln;
        int ci;
        int length;
        int cp;
        while ((ln = reader.readLine()) != null) {
            //过滤掉注释
            ci = ln.indexOf('#');
            if (ci >= 0) {
                ln = ln.substring(0, ci);
            }
            //过滤掉空格
            ln = ln.trim();
            length = ln.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    cp = ln.codePointAt(i);
                    if (i == 0 && !Character.isJavaIdentifierStart(cp)
                            || i > 0 && !Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                        //无效的java类
                        throw new PluginException(service.getName() + ": Illegal provider-class name: " + ln);
                    }
                }
                names.add(ln);
            }
        }
    }

    /**
     * 关闭
     *
     * @param closeable
     */
    protected void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException y) {
        }
    }

    /**
     * 加载插件类
     *
     * @param service
     * @param loader
     * @param className
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    protected Class loadPluginClass(final Class service, final ClassLoader loader, final String className)
            throws IllegalAccessException, InstantiationException {
        Class result = null;
        Class<? extends Annotation> annotationType;
        try {
            result = Class.forName(className, false, loader);
            if (!service.isAssignableFrom(result)) {
                //无效的java类
                throw new PluginException(service.getName() + "Provider " + className + " not a subtype");
            }
            Annotation[] annotations = result.getAnnotations();
            Conditional conditional;
            Condition condition;
            for (Annotation annotation : annotations) {
                annotationType = annotation.annotationType();
                if (annotationType == Conditional.class) {
                    conditional = (Conditional) annotation;
                } else {
                    conditional = annotationType.getAnnotation(Conditional.class);
                }
                if (conditional != null) {
                    for (Class clazz : conditional.value()) {
                        if (clazz != null) {
                            condition = (Condition) clazz.newInstance();
                            if (!condition.match(loader, clazz, annotation)) {
                                return null;
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
        }
        return result;
    }
}
