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

import io.joyrpc.spring.processor.DependsOnDefinitionPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将xml的标签绑定到解析器
 */
public class SpringNamespaceHandler extends NamespaceHandlerSupport {

    protected AtomicBoolean ready = new AtomicBoolean();

    @Override
    public void init() {
        registerBeanDefinitionParser("provider", new ProviderBeanDefinitionParser());
        registerBeanDefinitionParser("consumer", new ConsumerBeanDefinitionParser());
        registerBeanDefinitionParser("consumerGroup", new ConsumerGroupBeanDefinitionParser());
        registerBeanDefinitionParser("server", new ServerBeanDefinitionParser());
        registerBeanDefinitionParser("registry", new RegistryBeanDefinitionParser());
        registerBeanDefinitionParser("parameter", new GlobalParameterDefinitionParser());
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        if (ready.compareAndSet(false, true)) {
            BeanDefinitionRegistry registry = parserContext.getRegistry();
            if (!registry.containsBeanDefinition(DependsOnDefinitionPostProcessor.BEAN_NAME)) {
                registry.registerBeanDefinition(DependsOnDefinitionPostProcessor.BEAN_NAME,
                        new RootBeanDefinition(DependsOnDefinitionPostProcessor.class));
            }
        }
        return super.parse(element, parserContext);
    }

}
