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

import io.joyrpc.config.ParameterConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Converts;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * 全局解析
 */
public class GlobalParameterDefinitionParser implements BeanDefinitionParser {

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        RootBeanDefinition definition = new RootBeanDefinition();
        definition.setBeanClass(ParameterConfig.class);
        definition.setLazyInit(false);

        String key = element.getAttribute("key");
        String value = element.getAttribute("value");
        String hide = element.getAttribute("hide");

        Boolean hidden = Converts.getBoolean(hide, Boolean.FALSE);
        if (hidden) {
            GlobalContext.putIfAbsent(Constants.HIDE_KEY_PREFIX + key, value);
        } else {
            GlobalContext.putIfAbsent(key, value);
        }

        MutablePropertyValues values = definition.getPropertyValues();
        values.addPropertyValue("key", key);
        values.addPropertyValue("value", value);
        values.addPropertyValue("hide", hidden);

        return definition;
    }
}
