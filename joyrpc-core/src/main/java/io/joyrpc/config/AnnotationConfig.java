package io.joyrpc.config;

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

import java.io.Serializable;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 注解配置
 */
public class AnnotationConfig extends AbstractIdConfig implements Serializable {

    /*---------- 参数配置项开始 ------------*/

    /**
     * 包的基本路径（前缀）
     */
    protected String basepackage;

    /**
     * 是否扫描provider
     */
    protected boolean provider = true;

    /**
     * 是否扫描consumer
     */
    protected boolean consumer = true;

    /*---------- 参数配置项结束 ------------*/

    /**
     * 解析出来的各个包
     */
    protected transient String[] annotationPackages;

    public String getBasepackage() {
        return basepackage;
    }

    public void setBasepackage(String basepackage) {
        this.basepackage = basepackage;
        this.annotationPackages = basepackage == null || basepackage.isEmpty() ? null : split(basepackage, SEMICOLON_COMMA_WHITESPACE);
    }

    public boolean isProvider() {
        return provider;
    }

    public void setProvider(boolean provider) {
        this.provider = provider;
    }

    public boolean isConsumer() {
        return consumer;
    }

    public void setConsumer(boolean consumer) {
        this.consumer = consumer;
    }

    /**
     * 判断对象的package是否匹配
     *
     * @param bean
     * @return
     */
    protected boolean match(final Object bean) {
        if (bean == null) {
            return false;
        }
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        String beanClassName = bean.getClass().getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

}
