package io.joyrpc.codec.serialization;

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

import io.joyrpc.context.GlobalContext;
import io.joyrpc.permission.WhiteList;
import io.joyrpc.util.Resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static io.joyrpc.constants.Constants.DEFAULT_SERIALIZER_WHITELIST_ENABLED;
import static io.joyrpc.constants.Constants.SERIALIZER_WHITELIST_ENABLED;

/**
 * 序列化白名单，处理安全漏洞
 */
public class SerializerWhiteList implements WhiteList<String> {

    protected static final SerializerWhiteList GLOBAL_WHITELIST = new SerializerWhiteList(
            "META-INF/system_serialization_type", "META-INF/permission/serializer.whitelist").load();

    protected boolean enabled;

    protected Set<String> whites = new HashSet<>();

    protected String[] whiteListFiles;

    public SerializerWhiteList(String... whiteListFiles) {
        this.enabled = GlobalContext.getBoolean(SERIALIZER_WHITELIST_ENABLED, DEFAULT_SERIALIZER_WHITELIST_ENABLED);
        this.whiteListFiles = whiteListFiles;
    }

    /**
     * 加载本地白名单
     *
     * @return 黑白名单
     */
    public synchronized SerializerWhiteList load() {
        updateWhite(Resource.lines(whiteListFiles, false));
        return this;
    }

    /**
     * 设置白名单是否开启
     *
     * @param enabled 是否开启
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isWhite(String target) {
        return !enabled || whites.contains(target);
    }

    @Override
    public void updateWhite(Collection<String> targets) {
        if (targets != null && !targets.isEmpty()) {
            targets.forEach(target -> {
                if (target != null && !target.isEmpty()) {
                    whites.add(target);
                }
            });
        }
    }

    public static SerializerWhiteList getGlobalWhitelist() {
        return GLOBAL_WHITELIST;
    }
}
