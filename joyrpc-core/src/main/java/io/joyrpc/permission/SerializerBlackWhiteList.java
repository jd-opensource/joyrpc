package io.joyrpc.permission;

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

import java.util.Collection;

/**
 * 序列化黑白名单
 */
public class SerializerBlackWhiteList implements BlackWhiteList<Class<?>>, BlackList.BlackListAware, WhiteList.WhiteListAware {

    /**
     * 黑名单
     */
    protected SerializerBlackList blackList;

    /**
     * 白名单
     */
    protected SerializerWhiteList whiteList;

    /**
     * 构造方法
     *
     * @param blackListFiles 黑名单文件
     */
    public SerializerBlackWhiteList(String... blackListFiles) {
        this(new SerializerBlackList(blackListFiles), SerializerWhiteList.getGlobalWhitelist());
    }

    /**
     * 构造方法
     *
     * @param blackList 黑名单
     * @param whiteList 白名单
     */
    public SerializerBlackWhiteList(SerializerBlackList blackList, SerializerWhiteList whiteList) {
        this.blackList = blackList;
        this.whiteList = whiteList;
    }

    @Override
    public boolean isValid(Class<?> target) {
        while (target.isArray()) {
            target = target.getComponentType();
        }
        //枚举或者基本类型
        return target.isEnum() || target.isPrimitive() || (blackList == null || !blackList.isBlack(target))
                && (whiteList == null || whiteList.isWhite(target));
    }

    @Override
    public boolean isBlack(Class<?> target) {
        return blackList.isBlack(target);
    }

    @Override
    public void updateBlack(Collection<String> targets) {
        blackList.updateBlack(targets);
    }

    @Override
    public boolean isWhite(Class<?> target) {
        return whiteList.isWhite(target);
    }

    @Override
    public void updateWhite(Collection<String> targets) {
        whiteList.updateWhite(targets);
    }

}
