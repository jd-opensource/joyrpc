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

import io.joyrpc.util.Resource.Definition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static io.joyrpc.util.Resource.lines;

/**
 * 序列化黑名单，处理安全漏洞
 */
public class SerializerBlackList implements BlackList<Class<?>>, BlackList.BlackListAware {

    //合并后的黑名单
    protected volatile Set<Class<?>> blacks;
    //本地黑名单
    protected Set<Class<?>> locals;
    //远程黑名单
    protected Set<Class<?>> remotes;

    public SerializerBlackList(String... blackListFiles) {
        this.locals = add(new HashSet<>(200), lines(blackListFiles, true));
        this.blacks = merge(locals, remotes);
    }

    public SerializerBlackList(Definition[] blackListFiles) {
        this.locals = add(new HashSet<>(200), lines(blackListFiles, true));
        this.blacks = merge(locals, remotes);
    }

    @Override
    public synchronized void updateBlack(final Collection<String> targets) {
        remotes = add(new HashSet<>(targets == null ? 0 : targets.size()), targets);
        blacks = merge(locals, remotes);
    }

    /**
     * 合并
     *
     * @param locals  本地名单
     * @param remotes 远程名单
     * @return
     */
    protected Set<Class<?>> merge(final Set<Class<?>> locals, final Set<Class<?>> remotes) {
        int capacity = remotes != null ? remotes.size() : 0;
        capacity += locals != null ? locals.size() : 0;
        if (capacity == 0) {
            return null;
        }
        Set<Class<?>> result = new HashSet<>(capacity);
        if (locals != null) {
            result.addAll(locals);
        }
        if (remotes != null) {
            result.addAll(remotes);
        }
        return result;
    }

    /**
     * 添加到名单
     *
     * @param targets 名单
     * @param sources 待添加列表
     * @return 名单
     */
    protected Set<Class<?>> add(final Set<Class<?>> targets, final Collection<String> sources) {
        if (sources != null) {
            for (String source : sources) {
                if (source != null && !source.isEmpty()) {
                    try {
                        targets.add(Class.forName(source));
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return targets;
    }

    @Override
    public boolean isBlack(final Class clazz) {
        return blacks != null && blacks.contains(clazz);
    }
}
