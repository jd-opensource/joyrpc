package io.joyrpc.protocol.dubbo;

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

import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.util.StringUtils.split;

/**
 * Dubbo版本
 */
public class DubboVersion {

    /**
     * 最低版本
     */
    public static final int LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT = 2000200;
    protected static final Map<String, Integer> VERSION2INT = new HashMap<String, Integer>();

    /**
     * 获取版本
     *
     * @param version 版本字符串
     * @return 版本
     */
    public static int getIntVersion(String version) {
        Integer v = VERSION2INT.get(version);
        if (v == null) {
            int ver = 0;
            try {
                String[] parts = split(version, '.');
                int len = parts.length;
                for (int i = 0; i < len; i++) {
                    ver += Integer.parseInt(parts[i]) * Math.pow(10, (len - i - 1) * 2);
                }
                // 版本 2.6.3 转换为 2060300
                if (parts.length == 3) {
                    ver = ver * 100;
                }
            } catch (Exception e) {
                ver = LOWEST_VERSION_FOR_RESPONSE_ATTACHMENT;
            }
            v = ver;
            VERSION2INT.put(version, ver);
        }
        return v;
    }
}
