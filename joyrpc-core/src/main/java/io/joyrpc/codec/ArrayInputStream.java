package io.joyrpc.codec;

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

/**
 * 能直接读取自己数组的流
 */
public interface ArrayInputStream {

    /**
     * 是否有数组
     *
     * @return
     */
    boolean hasArray();

    /**
     * 数组
     *
     * @return
     */
    byte[] array();

    /**
     * 数据开始偏移量
     *
     * @return
     */
    int arrayOffset();

    /**
     * 读取索引
     *
     * @return
     */
    int readerIndex();
}
