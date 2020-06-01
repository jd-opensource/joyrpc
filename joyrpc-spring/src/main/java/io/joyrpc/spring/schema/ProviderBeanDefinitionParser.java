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

import io.joyrpc.spring.ProviderBean;
import io.joyrpc.util.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * ProviderBean解析器
 */
public class ProviderBeanDefinitionParser extends AbstractInterfaceBeanDefinitionParser {

    /**
     * 构造函数
     */
    public ProviderBeanDefinitionParser() {
        super(ProviderBean.class, true);
    }

    @Override
    protected void addCustomParser() {
        super.addCustomParser();
        parsers.put("registry", new RegistryParser());
        parsers.put("server", new ReferenceParser("serverConfig"));
    }

    /**
     * 解析注册中心引用
     */
    protected static class RegistryParser implements CustomParser {

        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            String value = element.getAttribute(name);
            if (!StringUtils.isEmpty(value)) {
                String[] beanIds = StringUtils.split(value, StringUtils.SEMICOLON_COMMA_WHITESPACE);
                ManagedList<BeanReference> registryList = new ManagedList();
                for (String beanId : beanIds) {
                    registryList.add(new RuntimeBeanNameReference(beanId));
                }
                definition.getPropertyValues().addPropertyValue("registry", registryList);
            }
        }
    }

}
