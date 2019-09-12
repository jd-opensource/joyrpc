package io.joyrpc.protocol.message.negotiation;

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

import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.protocol.message.Request;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.joyrpc.Plugin.*;

/**
 * 协商请求
 */
public class NegotiationRequest extends AbstractNegotiation implements Request {
    private static final long serialVersionUID = -7513047463450054884L;

    public NegotiationRequest() {
    }

    public NegotiationRequest(final String serialization, final String compression, final String checksum,
                              final List<String> serializations, final List<String> compressions, final List<String> checksums) {
        super(serialization, compression, checksum, serializations, compressions, checksums);
    }

    public NegotiationRequest(final URL url, final NegotiationOption serialization, final NegotiationOption compression,
                              final NegotiationOption checksum) {
        super(serialization == null ? null : serialization.extension(url, SERIALIZATION),
                compression == null ? null : compression.extension(url, COMPRESSION),
                checksum == null ? null : checksum.extension(url, CHECKSUM),
                serialization == null ? SERIALIZATION.names() : serialization.extensions(SERIALIZATION),
                compression == null ? COMPRESSION.names() : compression.extensions(COMPRESSION),
                checksum == null ? CHECKSUM.names() : checksum.extensions(CHECKSUM));
    }

    /**
     * 协商选项
     */
    public static class NegotiationOption {
        /**
         * URL参数
         */
        protected URLOption<String> option;
        /**
         * 排除的内容
         */
        protected Set<String> excludes;

        public NegotiationOption(final URLOption<String> option, final String... excludes) {
            this.option = option;
            if (excludes != null) {
                this.excludes = new HashSet<>(excludes.length);
                for (String exclude : excludes) {
                    this.excludes.add(exclude);
                }
            }
        }

        /**
         * 获取可用的插件
         *
         * @param extension 扩展点
         * @return
         */
        public List<String> extensions(final ExtensionPoint<?, String> extension) {
            List<String> result = extension == null ? new ArrayList<>(0) : extension.names();
            if (excludes != null && !excludes.isEmpty()) {
                result.removeAll(excludes);
            }
            return result;
        }

        /**
         * 获取默认插件
         *
         * @param url
         * @param extension
         * @return
         */
        public String extension(final URL url, final ExtensionPoint<?, String> extension) {
            String result = url == null ? null : url.getString(option);
            if (result != null && !result.isEmpty() && (excludes == null || !excludes.contains(result))
                    && extension != null && extension.get(result) != null) {
                //要判断插件是否存在
                return result;
            } else {
                return option.getValue();
            }
        }
    }
}
