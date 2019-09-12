package io.joyrpc.extension;

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
 * 裁剪接口
 */
public interface Strip {

    /**
     * 简单裁剪函数
     */
    Strip SIMPLE_STRIP = new SimpleStrip();

    /**
     * 应用
     *
     * @param prefix 前缀
     * @param value  完整值
     * @return
     */
    String apply(String prefix, String value);

    /**
     * 简单前缀裁剪
     */
    class SimpleStrip implements Strip {

        @Override
        public String apply(final String prefix, final String value) {
            return value.substring(prefix.length());
        }
    }
}
