package io.joyrpc.spring.util;

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

import org.springframework.beans.factory.ListableBeanFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;
import static org.springframework.util.ObjectUtils.containsElement;

public class BeanFactoryUtils {


    /**
     * Get optional Bean
     *
     * @param beanFactory {@link ListableBeanFactory}
     * @param beanName    the name of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return A bean if present , or <code>null</code>
     * @since 2.6.6
     */
    public static <T> T getOptionalBean(ListableBeanFactory beanFactory, String beanName, Class<T> beanType) {

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);

        if (!containsElement(allBeanNames, beanName)) {
            return null;
        }

        Map<String, T> beansOfType = beansOfTypeIncludingAncestors(beanFactory, beanType);

        return beansOfType.get(beanName);

    }


    /**
     * Gets name-matched Beans from {@link ListableBeanFactory BeanFactory}
     *
     * @param beanFactory {@link ListableBeanFactory BeanFactory}
     * @param beanNames   the names of Bean
     * @param beanType    the {@link Class type} of Bean
     * @param <T>         the {@link Class type} of Bean
     * @return the read-only and non-null {@link List} of Bean names
     * @since 2.6.6
     */
    public static <T> List<T> getBeans(ListableBeanFactory beanFactory, String[] beanNames, Class<T> beanType) {

        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);

        List<T> beans = new ArrayList<T>(beanNames.length);

        for (String beanName : beanNames) {
            if (containsElement(allBeanNames, beanName)) {
                beans.add(beanFactory.getBean(beanName, beanType));
            }
        }

        return Collections.unmodifiableList(beans);
    }
}
