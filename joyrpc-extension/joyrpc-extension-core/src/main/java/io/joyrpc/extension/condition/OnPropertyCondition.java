package io.joyrpc.extension.condition;

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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 环境变量和JVM参数判断
 */
public class OnPropertyCondition implements Condition {

    @Override
    public boolean match(final ClassLoader classLoader, final Class clazz, final Annotation annotation) {
        //从系统环境获取
        Map<String, String> env = new HashMap<>(System.getenv());
        //从系统属性和jvm参数获取
        System.getProperties().forEach((k, v) -> env.putIfAbsent(k.toString(), v.toString()));

        ConditionalOnProperty onProperty = (ConditionalOnProperty) annotation;
        String key = onProperty.name();
        String target = onProperty.value();
        if (key.isEmpty()) {
            //没有名称
            if (!target.isEmpty()) {
                //设置了值，则认为是name，其值应该是true或false
                return match(env, target, onProperty.matchIfMissing(), s -> Boolean.parseBoolean(s));
            } else {
                return false;
            }
        } else {
            return match(env, key, onProperty.matchIfMissing(), s -> target.isEmpty() ? Boolean.parseBoolean(s) : s.equals(target));
        }
    }

    /**
     * 匹配
     *
     * @param env            环境变量
     * @param key            键
     * @param matchIfMissing 不存在是否通过
     * @param predicate      断言
     * @return 匹配标识
     */
    protected boolean match(final Map<String, String> env,
                            final String key,
                            final boolean matchIfMissing,
                            final Predicate<String> predicate) {
        //设置了值，则认为是name，其值应该是true或false
        String value = env.get(key);
        if ((value == null || value.isEmpty()) && matchIfMissing) {
            return true;
        } else {
            return predicate.test(value);
        }
    }
}
