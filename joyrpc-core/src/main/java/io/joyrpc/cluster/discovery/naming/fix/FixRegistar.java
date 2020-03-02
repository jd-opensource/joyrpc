package io.joyrpc.cluster.discovery.naming.fix;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.discovery.naming.AbstractRegistar;
import io.joyrpc.cluster.discovery.naming.ClusterProvider;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static io.joyrpc.cluster.Shard.WEIGHT;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;

/**
 * 直连目录服务
 */
public class FixRegistar extends AbstractRegistar {

    protected static final String SHARD_KEY = "address";

    /**
     * 分片分隔符
     */
    protected Predicate<Character> delimiterPredicate;
    /**
     * 分片参数名称
     */
    protected String shardKey;

    public FixRegistar(final URL url) {
        this(url.getProtocol(), url, SEMICOLON_COMMA_WHITESPACE, SHARD_KEY);
    }

    public FixRegistar(final String name, final URL url) {
        this(name, url, SEMICOLON_COMMA_WHITESPACE, SHARD_KEY);
    }

    public FixRegistar(final String name, final URL url, final Predicate<Character> delimiterPredicate, final String shardKey) {
        super(name, url);
        this.delimiterPredicate = delimiterPredicate == null ? SEMICOLON_COMMA_WHITESPACE : delimiterPredicate;
        this.shardKey = shardKey == null || shardKey.isEmpty() ? SHARD_KEY : shardKey;
        this.provider = new URLProvider();
    }

    @Override
    public CompletableFuture<Void> open() {
        return switcher.openQuiet(() -> CompletableFuture.completedFuture(null));
    }

    @Override
    public CompletableFuture<Void> close() {
        return switcher.closeQuiet(() -> CompletableFuture.completedFuture(null));
    }

    @Override
    protected ClusterMeta create(final URL url, final String name) {
        //创建集群元数据，添加到任务队列
        ClusterMeta meta = new ClusterMeta(url, name);
        meta.setShards(provider.apply(this.url, url));
        return meta;
    }

    /**
     * 从URL参数获取集群节点
     */
    protected class URLProvider implements ClusterProvider {

        /**
         * 获取分片
         *
         * @param url
         * @return
         */
        protected List<Shard> apply(final URL url) {
            List<Shard> result = new LinkedList<>();
            String value = url == null ? null : url.getString(shardKey);
            if (value == null || value.isEmpty()) {
                value = url.getHost() + ":" + url.getPort();
            }
            String rg = url.getString(REGION, region);
            String dc = url.getString(DATA_CENTER, dataCenter);
            String name = url.getString("name");
            try {
                value = URL.decode(value);
            } catch (UnsupportedEncodingException ex) {
                throw new InitializationException("Value of \"url\" value is not encode in consumer config with key " + url + " !");
            }
            String[] shards = StringUtils.split(value, delimiterPredicate);
            int j = 0;
            URL nodeUrl;
            Parametric parametric = new MapParametric(GlobalContext.getContext());
            String defProtocol = parametric.getString(Constants.PROTOCOL_KEY);
            Integer defPort = url.getInteger(Constants.PORT_OPTION);
            for (String shard : shards) {
                nodeUrl = URL.valueOf(shard, defProtocol, defPort, null);
                result.add(new Shard.DefaultShard(name != null ? name + "-" + j++ : nodeUrl.getAddress(),
                        rg, dc, nodeUrl.getProtocol(), nodeUrl,
                        nodeUrl.getInteger(WEIGHT), Shard.ShardState.INITIAL));
            }
            return result;
        }

        @Override
        public List<Shard> apply(URL endpoint, URL cluster) {
            List<Shard> result = null;
            if (endpoint != null) {
                result = apply(endpoint);
            }
            if (result == null || result.isEmpty()) {
                result = apply(cluster);
            }
            return result;
        }
    }

}
