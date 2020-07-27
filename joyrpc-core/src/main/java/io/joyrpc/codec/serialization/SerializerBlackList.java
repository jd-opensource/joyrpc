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

import io.joyrpc.permission.BlackList;
import io.joyrpc.util.Resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 序列化黑名单，处理安全漏洞
 */
public class SerializerBlackList implements BlackList<String> {

    //合并后的黑名单
    protected volatile Set<String> blacks;
    //本地黑名单
    protected Set<String> locals;
    //远程黑名单
    protected Set<String> remotes;
    //本地黑名单文件候选者
    protected String[] blackListFiles;

    public SerializerBlackList(String... blackListFiles) {
        this.blackListFiles = blackListFiles;
        this.locals = add(new HashSet<>(200), Resource.lines(blackListFiles, false));
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
    protected Set<String> merge(final Set<String> locals, final Set<String> remotes) {
        int capacity = remotes != null ? remotes.size() : 0;
        capacity += locals != null ? locals.size() : 0;
        if (capacity == 0) {
            return null;
        }
        Set<String> result = new HashSet<>(capacity);
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
    protected Set<String> add(final Set<String> targets, final Collection<String> sources) {
        if (sources != null) {
            for (String target : sources) {
                if (target != null && !target.isEmpty()) {
                    targets.add(target);
                }
            }
        }
        return targets;
    }

    @Override
    public boolean isBlack(final String clazz) {
        return blacks != null && blacks.contains(clazz);
    }
}
