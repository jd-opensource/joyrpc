package io.joyrpc.cache.map;

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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Title: 带容量的<b>线程不安全的</b>最近访问排序的Hashmap<br>
 * <p/>
 * Description: 最后访问的元素在最后面。<br>
 * 如果要线程安全，请使用<pre>Collections.synchronizedMap(new LRUHashMap(123));</pre> <br>
 * <p/>
 */
public class LRUHashMap<K, V> extends LinkedHashMap<K, V> {

    /**
     * The Size.
     */
    private final int maxSize;

    /**
     * 初始化一个最大值, 按访问顺序排序
     *
     * @param maxSize the max size
     */
    public LRUHashMap(int maxSize) {
        //0.75是默认值，true表示按访问顺序排序
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }

    /**
     * 初始化一个最大值, 按指定顺序排序
     *
     * @param maxSize     最大值
     * @param accessOrder true表示按访问顺序排序，false为插入顺序
     */
    public LRUHashMap(int maxSize, boolean accessOrder) {
        //0.75是默认值，true表示按访问顺序排序，false为插入顺序
        super(maxSize, 0.75f, accessOrder);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return super.size() > maxSize;
    }

}
