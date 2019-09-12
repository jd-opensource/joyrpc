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
import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Method;
import io.joyrpc.spring.util.BeanFactoryUtils;
import io.joyrpc.spring.util.MethodConfigUtils;
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
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;


class ConsumerBeanBuilder {

    // Ignore those fields
    static final String[] IGNORE_FIELD_NAMES = new String[]{"registry", "methods"};


    protected final Log logger = LogFactory.getLog(getClass());

    protected final Consumer annotation;

    protected final ApplicationContext applicationContext;

    protected final ClassLoader classLoader;

    protected Object bean;

    protected Class<?> interfaceClass;

    private ConsumerBeanBuilder(Consumer annotation, ClassLoader classLoader, ApplicationContext applicationContext) {
        Assert.notNull(annotation, "The Annotation must not be null!");
        Assert.notNull(classLoader, "The ClassLoader must not be null!");
        Assert.notNull(applicationContext, "The ApplicationContext must not be null!");
        this.annotation = annotation;
        this.applicationContext = applicationContext;
        this.classLoader = classLoader;
    }

    public static ConsumerBeanBuilder create(Consumer annotation, ClassLoader classLoader, ApplicationContext applicationContext) {
        return new ConsumerBeanBuilder(annotation, classLoader, applicationContext);
    }

    public final ConsumerBean build() throws Exception {
        ConsumerBean bean = new ConsumerBean<Object>();
        configureBean(bean);
        logger.info(String.format("The bean type:%s has been built.", bean.getClass()));
        return bean;
    }

    public ConsumerBeanBuilder bean(Object bean) {
        this.bean = bean;
        return this;
    }

    public ConsumerBeanBuilder interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    protected void configureBean(ConsumerBean bean) throws Exception {

        preConfigureBean(bean);

        configureRegistryConfigs(bean);

        postConfigureBean(annotation, bean);

    }

    protected void preConfigureBean(ConsumerBean referenceBean) {
        Assert.notNull(interfaceClass, "The interface class must set first!");
        DataBinder dataBinder = new DataBinder(referenceBean);
        dataBinder.registerCustomEditor(Map.class, "parameters", new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws java.lang.IllegalArgumentException {
                String content = StringUtils.trimAllWhitespace(text);
                if (!StringUtils.hasText(content)) {
                    return;
                }
                // String[] to Map
                Map<String, String> parameters = MethodConfigUtils.toStringMap(commaDelimitedListToStringArray(content));
                setValue(parameters);
            }
        });
        dataBinder.bind(new MutablePropertyValues(getAttributes(annotation, applicationContext.getEnvironment(), true, IGNORE_FIELD_NAMES)));
    }

    protected void configureRegistryConfigs(ConsumerBean bean) {
        String registryConfigBeanIds = resolveRegistryConfigBeanNames(annotation);
        RegistryConfig registryConfigs = BeanFactoryUtils.getOptionalBean(applicationContext, registryConfigBeanIds, RegistryConfig.class);
        bean.setRegistry(registryConfigs);
    }

    protected void postConfigureBean(Consumer annotation, ConsumerBean bean) throws Exception {
        bean.setApplicationContext(applicationContext);
        configureInterface(annotation, bean);
        configureMethodConfig(annotation, bean);
        bean.afterPropertiesSet();
    }


    private void configureInterface(Consumer reference, ConsumerBean referenceBean) {
        referenceBean.setInterfaceClazz(this.interfaceClass.getName());
        referenceBean.setInterfaceClass(this.interfaceClass);
    }

    void configureMethodConfig(Consumer reference, ConsumerBean<?> referenceBean) {
        Method[] methods = reference.methods();
        Map<String, MethodConfig> configs = MethodConfigUtils.constructMethodConfig(methods);
        referenceBean.setMethods(configs);
    }


    protected String resolveRegistryConfigBeanNames(Consumer annotation) {
        return annotation.registry();
    }
}
