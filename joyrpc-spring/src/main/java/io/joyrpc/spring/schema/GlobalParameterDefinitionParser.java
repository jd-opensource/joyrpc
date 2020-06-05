package io.joyrpc.spring.schema;

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

import io.joyrpc.spring.Counter;
import io.joyrpc.spring.GlobalParameterBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Element;

import static io.joyrpc.spring.GlobalParameterBean.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * 全局解析
 */
public class GlobalParameterDefinitionParser implements BeanDefinitionParser {

    private Counter counter;

    /**
     * 注册全局参数
     *
     * @param registry 注册表
     * @param key      键
     * @param value    值
     */
    public static BeanDefinition register(final BeanDefinitionRegistry registry, final Counter counter, final String key,
                                          final Object value) {
        return register(registry, counter, key, value, null, null);
    }

    /**
     * 注册全局参数
     *
     * @param registry 注册表
     * @param key      键
     * @param value    值
     * @param ref      引用
     * @param hide     是否隐藏
     */
    public static BeanDefinition register(final BeanDefinitionRegistry registry, final Counter counter, final String key,
                                          final Object value, final String ref, final String hide) {
        BeanDefinitionBuilder builder = rootBeanDefinition(GlobalParameterBean.class).setLazyInit(false);
        builder.addPropertyValue(KEY, key);
        if (value != null) {
            builder.addPropertyValue(VALUE, value);
        } else if (ref != null && !ref.isEmpty()) {
            builder.addPropertyValue(VALUE, new RuntimeBeanReference(ref));
        }
        if (hide != null && !hide.isEmpty()) {
            builder.addPropertyValue(HIDE, hide);
        }
        AbstractBeanDefinition definition = builder.getBeanDefinition();
        registry.registerBeanDefinition("global-parameter-" + counter.incContext(), definition);
        return definition;
    }

    @Override
    public BeanDefinition parse(final Element element, final ParserContext parserContext) {
        if (counter == null) {
            ResourceLoader resourceLoader = parserContext.getReaderContext().getReader().getResourceLoader();
            ApplicationContext applicationContext = (ApplicationContext) resourceLoader;
            counter = Counter.computeCounter(applicationContext);
        }
        return register(parserContext.getRegistry(), counter, element.getAttribute(KEY),
                element.getAttribute(VALUE),
                element.getAttribute(REF),
                element.getAttribute(HIDE));
    }

}
