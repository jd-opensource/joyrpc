package io.joyrpc.spring.factory;

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

import io.joyrpc.config.MethodConfig;
import io.joyrpc.config.RegistryConfig;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Method;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

import java.beans.PropertyEditorSupport;
import java.util.Map;

import static io.joyrpc.spring.util.AnnotationUtils.getAttributes;
import static io.joyrpc.spring.util.BeanFactoryUtils.getOptionalBean;
import static io.joyrpc.spring.util.MethodConfigUtils.constructMethodConfig;
import static io.joyrpc.spring.util.MethodConfigUtils.toStringMap;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;


/**
 * 消费者Bean构造器
 */
class ConsumerBeanBuilder {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = new String[]{"registry", "methods"};

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * 消费者注解
     */
    protected Consumer annotation;
    /**
     * 应用上下文
     */
    protected ApplicationContext applicationContext;
    /**
     * 类加载器
     */
    protected ClassLoader classLoader;
    /**
     * 对象
     */
    protected Object bean;
    /**
     * 接口类
     */
    protected Class<?> interfaceClass;

    public static ConsumerBeanBuilder builder() {
        return new ConsumerBeanBuilder();
    }

    /**
     * 构建
     *
     * @return
     */
    public ConsumerBean build() {
        Assert.notNull(interfaceClass, "The interface class must set first!");
        Assert.notNull(annotation, "The Annotation must not be null!");
        Assert.notNull(classLoader, "The ClassLoader must not be null!");
        Assert.notNull(applicationContext, "The ApplicationContext must not be null!");
        ConsumerBean bean = new ConsumerBean<>();
        try {
            configureBean(bean);
            return bean;
        } catch (Exception e) {
            throw new InitializationException("Error occurs while build consumer bean,caused by " + e.getMessage(), e);
        }
    }

    public ConsumerBeanBuilder bean(Object bean) {
        this.bean = bean;
        return this;
    }

    public ConsumerBeanBuilder interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public ConsumerBeanBuilder annotation(Consumer annotation) {
        this.annotation = annotation;
        return this;
    }

    public ConsumerBeanBuilder classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public ConsumerBeanBuilder applicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    /**
     * 进行配置
     *
     * @param bean
     * @throws Exception
     */
    protected void configureBean(ConsumerBean bean) throws Exception {
        DataBinder dataBinder = new DataBinder(bean);
        dataBinder.registerCustomEditor(Map.class, "parameters", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws java.lang.IllegalArgumentException {
                String content = StringUtils.trimAllWhitespace(text);
                if (!StringUtils.hasText(content)) {
                    return;
                }
                // String[] to Map
                Map<String, String> parameters = toStringMap(commaDelimitedListToStringArray(content));
                setValue(parameters);
            }
        });
        dataBinder.bind(new MutablePropertyValues(getAttributes(annotation, applicationContext.getEnvironment(), true, IGNORE_FIELD_NAMES)));
        bean.setRegistry(getOptionalBean(applicationContext, annotation.registry(), RegistryConfig.class));
        bean.setApplicationContext(applicationContext);
        bean.setInterfaceClazz(interfaceClass.getName());
        bean.setInterfaceClass(interfaceClass);
        Method[] methods = annotation.methods();
        Map<String, MethodConfig> configs = constructMethodConfig(methods);
        bean.setMethods(configs);
        bean.afterPropertiesSet();
    }
}
