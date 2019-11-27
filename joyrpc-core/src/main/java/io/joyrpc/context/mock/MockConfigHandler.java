package io.joyrpc.context.mock;

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


import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.GLOBAL_SETTING;
import static io.joyrpc.constants.Constants.SETTING_INVOKE_MOCKRESULT;
import static io.joyrpc.context.ConfigEventHandler.BIZ_ORDER;
import static io.joyrpc.context.mock.MockConfiguration.MOCK;
import static io.joyrpc.util.ClassUtils.forNameQuiet;
import static io.joyrpc.util.ClassUtils.getPublicMethod;


/**
 * Mock数据管理器
 *
 * @date: 2019/6/21
 */
@Extension(value = "mock", order = BIZ_ORDER)
public class MockConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(MockConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (!GLOBAL_SETTING.equals(className)) {
            String oldAttr = oldAttrs.get(SETTING_INVOKE_MOCKRESULT);
            String newAttr = newAttrs.get(SETTING_INVOKE_MOCKRESULT);
            if (!Objects.equals(oldAttr, newAttr)) {
                //泛化调用，客户端没有类
                Class clazz = forNameQuiet(className);
                List<Map> results = parse(newAttr);
                Map<String, Map<String, Object>> configs = null;
                if (results != null) {
                    configs = new HashMap<>(results.size());
                    for (Map result : results) {
                        String alias = (String) result.get("alias");
                        String methodName = (String) result.get("method");
                        String value = (String) result.get("mockValue");
                        if (!StringUtils.isEmpty(alias) && !StringUtils.isEmpty(methodName)) {
                            try {
                                //泛化调用，客户端没有类
                                Method method = clazz == null ? null : getPublicMethod(clazz, methodName);
                                Object mock = JSON.get().parseObject(value, method != null ? method.getGenericReturnType() : Map.class);
                                configs.computeIfAbsent(methodName, o -> new HashMap<>()).put(alias, mock);
                            } catch (NoSuchMethodException e) {
                                logger.error(String.format("Error occurs while update mock. no method %s of %s", methodName, className));
                            } catch (MethodOverloadException e) {
                                logger.error(String.format("Error occurs while update mock. overload method %s of %s", methodName, className));
                            } catch (SerializerException e) {
                                logger.error(String.format("Error occurs while update mock. invalid mock value of method %s of %s", methodName, className));
                            }
                        }
                    }
                }
                MOCK.update(className, configs);
            }

        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{SETTING_INVOKE_MOCKRESULT};
    }

    /**
     * 解析
     *
     * @param text
     * @return
     */
    protected List<Map> parse(final String text) {
        try {
            return text == null || text.isEmpty() ? null : JSON.get().parseObject(text, new TypeReference<List<Map>>() {
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

}
