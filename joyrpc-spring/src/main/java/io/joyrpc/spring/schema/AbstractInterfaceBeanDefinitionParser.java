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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static io.joyrpc.util.StringUtils.isEmpty;

/**
 * 接口抽象解析类
 */
public class AbstractInterfaceBeanDefinitionParser extends AbstractBeanDefinitionParser {

    public static final String PARAMETER = "parameter";
    public static final String METHOD = "method";

    public AbstractInterfaceBeanDefinitionParser(Class<?> beanClass, boolean requireId) {
        super(beanClass, requireId);
    }

    @Override
    protected void addCustomParser() {
        super.addCustomParser();
        parsers.put("methods", new MethodParser());
    }

    /**
     * 方法解析器
     */
    public static class MethodParser implements CustomParser {
        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            NodeList nodes = element.getChildNodes();
            if (nodes != null && nodes.getLength() > 0) {
                ManagedList methods = new ManagedList();
                Node node;
                String methodName;
                MethodBeanDefinitionParser parser = new MethodBeanDefinitionParser();
                for (int i = 0; i < nodes.getLength(); i++) {
                    node = nodes.item(i);
                    if (node instanceof Element && (METHOD.equals(node.getNodeName()) || METHOD.equals(node.getLocalName()))) {
                        methodName = ((Element) node).getAttribute("name");
                        if (isEmpty(methodName)) {
                            throw new IllegalStateException("method name attribute == null");
                        }
                        methods.add(new BeanDefinitionHolder(
                                parser.parse(((Element) node), context), id + "." + methodName));
                    }
                }
                if (!methods.isEmpty()) {
                    definition.getPropertyValues().addPropertyValue(name, methods);
                }
            }
        }
    }
}
