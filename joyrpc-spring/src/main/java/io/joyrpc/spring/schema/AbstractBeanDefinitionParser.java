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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.extension.Converts;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.joyrpc.util.ClassUtils.getPublicMethod;
import static io.joyrpc.util.StringUtils.*;

/**
 * 解析类
 */
public class AbstractBeanDefinitionParser implements BeanDefinitionParser {

    public static final String PARAMETER = "parameter";
    /**
     * 类型
     */
    protected final Class<?> beanClass;
    /**
     * 是否需要ID
     */
    protected final boolean requireId;
    /**
     * 自定义解析器
     */
    protected Map<String, CustomParser> parsers = new HashMap<>();

    /**
     * 构造函数
     *
     * @param beanClass
     * @param requireId
     */
    public AbstractBeanDefinitionParser(Class<?> beanClass, boolean requireId) {
        this.beanClass = beanClass;
        this.requireId = requireId;
        addCustomParser();
    }

    /**
     * 添加自定义解析器
     */
    protected void addCustomParser() {
        parsers.put("parameters", new ParameterParser());
    }

    @Override
    public BeanDefinition parse(final Element element, final ParserContext context) {
        RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClass(beanClass);
        definition.setLazyInit(false);
        String id = element.getAttribute("id");

        if (requireId) {
            if (isEmpty(id)) {
                throw new IllegalConfigureException("spring.bean", id, "do not set", ExceptionCode.COMMON_VALUE_ILLEGAL);
            } else {
                if (context.getRegistry().containsBeanDefinition(id)) {
                    throw new IllegalConfigureException("spring.bean", id, "duplicate spring bean id", ExceptionCode.COMMON_VALUE_ILLEGAL);
                }
                context.getRegistry().registerBeanDefinition(id, definition);
            }
        }
        //set各个属性值
        String methodName;
        String property;
        CustomParser parser;
        List<Method> methods = getPublicMethod(beanClass);
        for (Method setter : methods) {
            //略过不是property的方法
            if (!isSetter(setter)) {
                continue;
            }
            methodName = setter.getName();
            property = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
            parser = parsers.get(property);
            if (parser != null) {
                parser.parse(definition, id, element, property, context);
            } else {
                String value = element.getAttribute(property);
                if (!isEmpty(value)) {
                    definition.getPropertyValues().addPropertyValue(property, value);
                }
            }
        }

        return definition;
    }

    /**
     * 判断是否是Setter方法
     */
    protected boolean isSetter(final Method method) {
        //调用该方法已经确保是public
        String methodName = method.getName();
        return methodName.length() > 3 && methodName.startsWith("set") && method.getParameterCount() == 1;
    }

    /**
     * 自定义解析器
     */
    public interface CustomParser {
        /**
         * 解析
         *
         * @param definition Bean定义
         * @param id         beanId
         * @param element    XML元素
         * @param name       字段名称
         * @param context    上下文
         */
        void parse(BeanDefinition definition, String id, Element element, String name, ParserContext context);
    }

    /**
     * 名称转义解析器
     */
    public static class MappingParser implements CustomParser {
        /**
         * 配置名称
         */
        protected String attribute;

        /**
         * 构造函数
         *
         * @param attribute
         */
        public MappingParser(String attribute) {
            this.attribute = attribute;
        }

        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            String value = element.getAttribute(attribute);
            if (!isEmpty(value)) {
                definition.getPropertyValues().addPropertyValue(name, value);
            }
        }
    }

    /**
     * 引用
     */
    public static class ReferenceParser implements CustomParser {

        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            String value = element.getAttribute(name);
            if (!isEmpty(value)) {
                definition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(value));
            }
        }
    }

    /**
     * 逗号分隔的多个引用
     */
    public static class MultiReferenceParser implements CustomParser {

        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            String value = element.getAttribute(name);
            if (!isEmpty(value)) {
                String[] values = split(value, SEMICOLON_COMMA_WHITESPACE);
                ManagedList list = new ManagedList();
                for (String v : values) {
                    list.add(new RuntimeBeanReference(v));
                }
                definition.getPropertyValues().addPropertyValue(name, list);
            }
        }
    }

    /**
     * 参数解析器
     */
    public static class ParameterParser implements CustomParser {
        @Override
        public void parse(final BeanDefinition definition, final String id, final Element element, final String name,
                          final ParserContext context) {
            NodeList nodes = element.getChildNodes();
            if (nodes != null && nodes.getLength() > 0) {
                ManagedMap parameters = new ManagedMap();
                Node node;
                String key;
                String value;
                boolean hidden;
                for (int i = 0; i < nodes.getLength(); i++) {
                    node = nodes.item(i);
                    if (node instanceof Element && (PARAMETER.equals(node.getNodeName())
                            || PARAMETER.equals(node.getLocalName()))) {
                        key = ((Element) node).getAttribute("key");
                        if (!RequestContext.VALID_KEY.test(key)) {
                            throw new IllegalConfigureException("param.key", key, "key can not start with "
                                    + Constants.HIDE_KEY_PREFIX + " and " + Constants.INTERNAL_KEY_PREFIX, ExceptionCode.COMMON_ABUSE_HIDE_KEY);
                        }
                        value = ((Element) node).getAttribute("value");
                        hidden = Converts.getBoolean(((Element) node).getAttribute("hide"), Boolean.FALSE);
                        if (hidden) {
                            key = Constants.HIDE_KEY_PREFIX + key;
                        }
                        parameters.put(key, new TypedStringValue(value, String.class));
                    }
                }
                if (!parameters.isEmpty()) {
                    definition.getPropertyValues().addPropertyValue(name, parameters);
                }
            }
        }
    }
}
