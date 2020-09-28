package io.joyrpc.protocol.http.injection;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.http.HeaderInjection;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.Resource;
import io.joyrpc.util.Resource.Definition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static io.joyrpc.util.StringUtils.split;


/**
 * 默认参数注入
 */
@Extension("default")
public class DefaultHeaderInjection implements HeaderInjection {

    protected List<PrefixKey> prefixKeys = new ArrayList<>(5);
    protected List<EqualKey> equalKeys = new ArrayList<>(15);

    public DefaultHeaderInjection() {
        List<String> lines = Resource.lines(new Definition[]{
                new Definition("META-INF/system_http_header", true),
                new Definition("system_http_header")
        }, true);
        String[] parts;
        for (String line : lines) {
            parts = split(line, '=');
            if (parts.length == 2) {
                if (parts[0].endsWith("*")) {
                    prefixKeys.add(new PrefixKey(parts[0].substring(0, parts[0].length() - 1), parts[1]));
                } else {
                    equalKeys.add(new EqualKey(parts[0], parts[1]));
                }
            } else {
                equalKeys.add(new EqualKey(parts[0], parts[0]));
            }
        }
    }

    @Override
    public void inject(final Invocation invocation, final Parametric header) {
        if (!equalKeys.isEmpty()) {
            String value;
            for (EqualKey equalKey : equalKeys) {
                value = header.getString(equalKey.key);
                if (value != null && !value.isEmpty()) {
                    invocation.addAttachment(equalKey.replace, value);
                }
            }
        }
        header.foreach((key, value) -> {
            if (!key.isEmpty() && !prefixKeys.isEmpty()) {
                String replace;
                for (PrefixKey prefixKey : prefixKeys) {
                    replace = prefixKey.apply(key);
                    if (replace != null && !replace.isEmpty()) {
                        invocation.addAttachment(replace, value);
                        break;
                    }
                }
            }
        });
    }

    /**
     * 前缀匹配
     */
    protected static class PrefixKey implements Function<String, String> {
        /**
         * 前缀
         */
        protected String prefix;
        /**
         * 替换
         */
        protected String replace;

        public PrefixKey(String prefix, String replace) {
            this.prefix = prefix;
            this.replace = replace;
        }

        @Override
        public String apply(String s) {
            return s.startsWith(prefix) ? replace.replace("*", s.substring(prefix.length())) : null;
        }
    }

    /**
     * 键匹配
     */
    protected static class EqualKey implements Function<String, String> {
        /**
         * 键
         */
        protected String key;
        /**
         * 替换
         */
        protected String replace;

        public EqualKey(String key, String replace) {
            this.key = key;
            this.replace = replace;
        }

        @Override
        public String apply(String s) {
            return key.equals(s) ? replace : null;
        }
    }

}
