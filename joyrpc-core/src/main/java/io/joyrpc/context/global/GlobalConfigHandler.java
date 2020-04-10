package io.joyrpc.context.global;

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


import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.permission.BlackList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.Plugin.SERIALIZATION;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.ConfigEventHandler.GLOBAL_ORDER;


/**
 * 注册中心全局配置参数处理handler
 *
 * @date: 2019/6/21
 */
@Extension(value = "global", order = GLOBAL_ORDER)
public class GlobalConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalConfigHandler.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (GLOBAL_SETTING.equals(className)) {
            newAttrs.putIfAbsent(SETTING_REGISTRY_HEARTBEAT_INTERVAL, "15000");
            newAttrs.putIfAbsent(SETTING_REGISTRY_CHECK_INTERVAL, "300000");
            //修改回调线程池
            InvokerManager.updateThreadPool(InvokerManager.getCallbackThreadPool(), "callback",
                    new MapParametric(newAttrs),
                    SETTING_CALLBACK_POOL_CORE_SIZE,
                    SETTING_CALLBACK_POOL_MAX_SIZE);
            updateSerializationBlackList(oldAttrs, newAttrs);
        }
    }

    /**
     * 修改序列化黑白名单
     *
     * @param oldAttrs 老的配置
     * @param newAttrs 新的配置
     */
    protected void updateSerializationBlackList(final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        //修改序列化的黑白名单
        String newBlackLists = newAttrs.get(SETTING_SERIALIZATION_BLACKLIST);
        String oldBlackLists = oldAttrs == null ? null : oldAttrs.get(SETTING_SERIALIZATION_BLACKLIST);
        if (!Objects.equals(newBlackLists, oldBlackLists)) {
            if (newBlackLists == null) {
                SERIALIZATION.extensions().forEach(o -> updateSerializationBlackList(o, null));
            } else {
                try {
                    Map<String, List<String>> configs = JSON.get().parseObject(newBlackLists, new TypeReference<Map<String, List<String>>>() {
                    });
                    configs.forEach((type, blackList) -> updateSerializationBlackList(SERIALIZATION.get(type), blackList));
                } catch (SerializerException e) {
                    logger.error("Error occurs while parsing serialization blacklist config.\n" + newBlackLists);
                }
            }
        }
    }

    /**
     * 修改序列化黑名单
     *
     * @param serialization 序列化
     * @param blackList     黑名单
     */
    protected void updateSerializationBlackList(final Serialization serialization, final List<String> blackList) {
        if (serialization instanceof BlackList.BlackListAware) {
            ((BlackList.BlackListAware) serialization).updateBlack(blackList);
        }
    }


}
