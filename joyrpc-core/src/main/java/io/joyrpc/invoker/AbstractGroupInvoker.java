package io.joyrpc.invoker;

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

import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 参数分组路由
 */
public abstract class AbstractGroupInvoker implements GroupInvoker {
    /**
     * URL
     */
    protected URL url;
    /**
     * 接口类
     */
    protected Class clazz;
    /**
     * 类名
     */
    protected String className;
    /**
     * 别名配置
     */
    protected String alias;
    /**
     * 消费者函数
     */
    protected Function<String, ConsumerConfig<?>> consumerFunction;
    /**
     * 分组别名
     */
    protected volatile AliasMeta aliasMeta;
    /**
     * 分组下调用者配置
     */
    protected Map<String, ConsumerConfig<?>> configMap = new ConcurrentHashMap<>();

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void setAlias(final String alias) {
        this.alias = alias;
    }

    @Override
    public void setConfigFunction(final Function<String, ConsumerConfig<?>> function) {
        this.consumerFunction = function;
    }

    @Override
    public void setClass(final Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setClassName(final String className) {
        this.className = className;
    }

    @Override
    public void setup() {
        aliasMeta = new AliasMeta(alias);
    }

    @Override
    public CompletableFuture<Void> refer() {
        //创建消费引用
        CompletableFuture<Void>[] futures = new CompletableFuture[aliasMeta.size()];
        int i = 0;
        for (String alias : aliasMeta.arrays) {
            futures[i] = new CompletableFuture<>();
            configMap.computeIfAbsent(alias, consumerFunction).refer(futures[i]);
            i++;
        }
        return CompletableFuture.allOf(futures);
    }

    @Override
    public CompletableFuture<Void> close() {
        Parametric parametric = new MapParametric(GlobalContext.getContext());
        return close(parametric.getBoolean(Constants.GRACEFULLY_SHUTDOWN_OPTION));
    }

    @Override
    public CompletableFuture<Void> close(final boolean gracefully) {
        Map<String, ConsumerConfig<?>> configs = new HashMap<>(configMap);
        CompletableFuture<Void>[] futures = new CompletableFuture[configs.size()];
        int i = 0;
        for (ConsumerConfig<?> config : configs.values()) {
            futures[i++] = config.unrefer(gracefully);
        }
        return CompletableFuture.allOf(futures);
    }

    /**
     * 别名元数据
     */
    public static class AliasMeta {

        /**
         * 分组别名
         */
        protected String[] arrays;
        /**
         * 别名
         */
        protected Set<String> aliases;

        /**
         * 构造函数
         *
         * @param alias 分组
         */
        public AliasMeta(final String alias) {
            //计算别名，去重
            arrays = split(alias, SEMICOLON_COMMA_WHITESPACE);
            aliases = new LinkedHashSet<>(arrays.length);
            for (String name : arrays) {
                aliases.add(name);
            }
            if (arrays.length != aliases.size()) {
                arrays = aliases.isEmpty() ? new String[0] : aliases.toArray(new String[aliases.size()]);
            }
        }

        /**
         * 构造函数
         *
         * @param aliases
         */
        public AliasMeta(final Set<String> aliases) {
            this.aliases = aliases;
            this.arrays = aliases.isEmpty() ? new String[0] : aliases.toArray(new String[aliases.size()]);
        }

        /**
         * 随机选择
         *
         * @return 目标分组名称
         */
        public String random() {
            int size = arrays.length;
            return size == 1 ? arrays[0] : arrays[ThreadLocalRandom.current().nextInt(size)];
        }

        public String[] getArrays() {
            return arrays;
        }

        /**
         * 条数
         *
         * @return 条数
         */
        public int size() {
            return arrays.length;
        }
    }


}
