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

import io.joyrpc.spring.factory.ConsumerAnnotationBeanPostProcessor;
import io.joyrpc.spring.factory.ProviderAnnotationBeanPostProcessor;
import io.joyrpc.spring.util.BeanRegistrarUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.trimArrayElements;

/**
 * annotation解析器
 */
public class AnnotationBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String basePackage = element.getAttribute("package");
        String[] basePackages = trimArrayElements(commaDelimitedListToStringArray(basePackage));
        builder.addConstructorArgValue(basePackages);

        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        registerConsumerAnnotationBeanPostProcessor(parserContext.getRegistry());
    }

    private void registerConsumerAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {
        BeanRegistrarUtils.registerInfrastructureBean(registry,
                ConsumerAnnotationBeanPostProcessor.BEAN_NAME, ConsumerAnnotationBeanPostProcessor.class);

    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return ProviderAnnotationBeanPostProcessor.class;
    }

}
