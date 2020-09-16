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

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.spring.ConsumerGroupBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ConsumerGroupBean解析器
 */
public class ConsumerGroupBeanDefinitionParser extends AbstractInterfaceBeanDefinitionParser {

    /**
     * 构造函数
     */
    public ConsumerGroupBeanDefinitionParser() {
        super(ConsumerGroupBean.class, true);
    }

    @Override
    protected void addCustomParser() {
        super.addCustomParser();
        parsers.put("consumerConfigs", new ConsumerParser());
    }


    /**
     * 方法解析器
     */
    public static class ConsumerParser implements CustomParser {

        private ConsumerConfigBeanDefinitionParser consumerBeanDefinitionParser = new ConsumerConfigBeanDefinitionParser();

        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            NodeList nodeList = element.getChildNodes();
            if (nodeList != null && nodeList.getLength() > 0) {
                ManagedMap consumerConfigs = null;
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node instanceof Element) {
                        Element elt = (Element) node;
                        if ("consumer".equals(node.getNodeName()) || "consumer".equals(node.getLocalName())) {
                            String alias = elt.getAttribute("alias");
                            if (alias == null || alias.isEmpty()) {
                                throw new IllegalStateException("Attribute alias of <jsf:consumer> below <jsf:consumerGroup> is empty");
                            }
                            BeanDefinition consumerBeanDefinition = consumerBeanDefinitionParser.parse(elt, context);
                            if (consumerBeanDefinition != null) {
                                BeanDefinitionHolder consumerBeanDefinitionHolder = new BeanDefinitionHolder(
                                        consumerBeanDefinition, id + "." + alias);
                                if (consumerConfigs == null) {
                                    consumerConfigs = new ManagedMap();
                                }
                                if (consumerConfigs.containsKey(alias)) {
                                    throw new IllegalConfigureException("consumerGroup.alias", alias,
                                            "Duplicate alias in consumer group of " + id, "21318");
                                }
                                consumerConfigs.put(alias, consumerBeanDefinitionHolder);
                            }
                        }
                    }
                }
                if (consumerConfigs != null) {
                    definition.getPropertyValues().addPropertyValue("consumerConfigs", consumerConfigs);
                }
            }
        }
    }

    public static class ConsumerConfigBeanDefinitionParser extends AbstractInterfaceBeanDefinitionParser {

        public ConsumerConfigBeanDefinitionParser() {
            super(ConsumerConfig.class, false);
        }
    }


}
