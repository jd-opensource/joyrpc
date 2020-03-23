package io.joyrpc.context.auth;

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


import io.joyrpc.context.ConfigEventHandler;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.Extension;
import io.joyrpc.util.network.Lan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.ConfigEventHandler.PERMISSION_ORDER;
import static io.joyrpc.context.auth.IPPermissionConfiguration.IP_PERMISSION;


/**
 * IP黑白名单
 */
@Extension(value = "auth", order = PERMISSION_ORDER)
public class IPPermissionConfigHandler implements ConfigEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(IPPermissionConfiguration.class);

    @Override
    public void handle(final String className, final Map<String, String> oldAttrs, final Map<String, String> newAttrs) {
        if (!GLOBAL_SETTING.equals(className)) {
            String oldOpen = oldAttrs.getOrDefault(SETTING_INVOKE_WB_OPEN, "true");
            String newOpen = newAttrs.getOrDefault(SETTING_INVOKE_WB_OPEN, "true");
            String oldBlacklist = oldAttrs.getOrDefault(SETTING_INVOKE_BLACKLIST, "");
            String newBlacklist = newAttrs.getOrDefault(SETTING_INVOKE_BLACKLIST, "");
            String oldWhitelist = oldAttrs.getOrDefault(SETTING_INVOKE_WHITELIST, "{\"*\":\"*\"}");
            String newWhiteList = newAttrs.getOrDefault(SETTING_INVOKE_WHITELIST, "{\"*\":\"*\"}");
            if (!Objects.equals(oldOpen, newOpen)
                    || !Objects.equals(oldBlacklist, newBlacklist)
                    || !Objects.equals(oldWhitelist, newWhiteList)) {
                //重新构建
                boolean enabled = Converts.getBoolean(newOpen, Boolean.TRUE);
                if (!enabled) {
                    IP_PERMISSION.remove(className);
                } else {
                    //许可`
                    IPPermission permission = new IPPermission(true, parse(className, newWhiteList), parse(className, newBlacklist));
                    //修改黑白名单
                    IP_PERMISSION.update(className, permission);
                }
            }
        }
    }

    @Override
    public String[] getKeys() {
        return new String[]{SETTING_INVOKE_WB_OPEN, SETTING_INVOKE_BLACKLIST, SETTING_INVOKE_WHITELIST};
    }

    /**
     * 解析分组的黑白名单
     *
     * @param interfaceId 接口ID
     * @param text        文本
     * @return 分组的黑白名单
     */
    protected Map<String, Lan> parse(final String interfaceId, final String text) {
        if (text == null || text.isEmpty() || "*".equals(text)) {
            return new HashMap<>();
        }
        //判断是否是JSON格式
        int pos = text.indexOf(':');
        if (pos > 0) {
            try {
                Map<String, String> map = JSON.get().parseObject(text, Map.class);
                Map<String, Lan> result = new HashMap<>(map.size());
                map.forEach((k, v) -> result.put(k, new Lan(v == null ? IPPermission.MASK : v, true)));
                return result;
            } catch (Exception e) {
                logger.error("Error occurs while parsing ip blackWhiteList of " + interfaceId, e);
                return new HashMap<>();
            }
        } else {
            //IP列表，对所有分组有效
            Map<String, Lan> result = new HashMap<>();
            result.put(IPPermission.MASK, new Lan(text, true));
            return result;
        }
    }
}
