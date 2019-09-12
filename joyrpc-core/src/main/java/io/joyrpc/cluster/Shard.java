package io.joyrpc.cluster;

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

import io.joyrpc.context.Environment;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;

import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * 分片信息
 */
public interface Shard extends Weighter, Region, Comparable<Shard> {

    /**
     * 默认权重
     */
    URLOption<Integer> WEIGHT = new URLOption<>("weight", 100);

    /**
     * 名称
     *
     * @return name
     */
    String getName();

    /**
     * 协议
     *
     * @return protocol
     */
    String getProtocol();

    /**
     * 地址
     *
     * @return url
     */
    URL getUrl();

    /**
     * 权重
     *
     * @return weight
     */
    int getWeight();

    /**
     * 获取状态
     *
     * @return state
     */
    default ShardState getState() {
        return ShardState.INITIAL;
    }

    @Override
    default int compareTo(Shard o) {
        if (o == null && this == null) {
            return 0;
        } else if (o == null) {
            return -1;
        } else if (this == null) {
            return 1;
        }
        //根据状态进行排序
        int result = getState().getOrder() - o.getState().getOrder();
        if (result == 0) {
            //状态相同，则根据权重排序
            result = getWeight() - o.getWeight();
            //状态相同，随机排序
            //result = ThreadLocalRandom.current().nextInt(-1, 2);
        }
        return result;
    }

    /**
     * 分片状态
     */
    enum ShardState {

        /**
         * 初始化.
         */
        INITIAL(0, 4, "初始状态") {
            @Override
            public boolean candidate(Consumer<ShardState> consumer) {
                consumer.accept(CANDIDATE);
                return true;
            }
        },

        /**
         * 候选.
         */
        CANDIDATE(1, 3, "候选状态") {
            @Override
            public boolean connecting(Consumer<ShardState> consumer) {
                consumer.accept(CONNECTING);
                return true;
            }
        },

        /**
         * 连接中
         */
        CONNECTING(2, 1, "连接中") {
            @Override
            public boolean connected(final Consumer<ShardState> consumer) {
                consumer.accept(CONNECTED);
                return true;
            }

            @Override
            public boolean disconnect(final Consumer<ShardState> consumer) {
                consumer.accept(DISCONNECT);
                return true;
            }
        },

        /**
         * 连接好，健康.
         */
        CONNECTED(3, 0, "健康的") {
            @Override
            public boolean weak(final Consumer<ShardState> consumer) {
                consumer.accept(WEAK);
                return true;
            }

            @Override
            public boolean disconnect(final Consumer<ShardState> consumer) {
                consumer.accept(DISCONNECT);
                return true;
            }

        },

        /**
         * 亚健康.
         */
        WEAK(4, 2, "亚健康的") {
            @Override
            public boolean connected(final Consumer<ShardState> consumer) {
                consumer.accept(CONNECTED);
                return true;
            }

            @Override
            public boolean disconnect(final Consumer<ShardState> consumer) {
                consumer.accept(DISCONNECT);
                return true;
            }

        },

        /**
         * 连接断开，可以重连.
         */
        DISCONNECT(5, 5, "死亡的") {
            @Override
            public boolean initial(final Consumer<ShardState> consumer) {
                //新加的节点，原有死亡状态可以切换到初始状态
                consumer.accept(INITIAL);
                return true;
            }

            @Override
            public boolean connecting(final Consumer<ShardState> consumer) {
                consumer.accept(CONNECTING);
                return true;
            }

            @Override
            public boolean disconnect(Consumer<ShardState> consumer) {
                consumer.accept(DISCONNECT);
                return true;
            }
        };


        private int type;
        private int order;
        private String desc;

        ShardState(int type, int order, String desc) {
            this.type = type;
            this.desc = desc;
            this.order = order;
        }

        /**
         * Gets type.
         *
         * @return the type
         */
        public int getType() {
            return type;
        }

        public int getOrder() {
            return order;
        }

        /**
         * Gets desc.
         *
         * @return the desc
         */
        public String getDesc() {
            return desc;
        }

        /**
         * set Initial State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean initial(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Candidate State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean candidate(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Connecting State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean connecting(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Connected State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean connected(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Weak State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean weak(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Disconnect State.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean disconnect(Consumer<ShardState> consumer) {
            return false;
        }

        /**
         * set Disconnect State 关闭状态.
         *
         * @param consumer the consumer
         * @return the boolean
         */
        public boolean close(Consumer<ShardState> consumer) {
            consumer.accept(DISCONNECT);
            return true;
        }

    }

    /**
     * 当前服务健康状态，0：可用，非0：不可用
     *
     * @return
     */
    default Health getHealth() {
        switch (getState()) {
            case CONNECTED:
                return Health.NORMAL;
            case WEAK:
                return Health.ABNORMAL;
            default:
                return Health.FORBIDDEN;
        }
    }

    /**
     * 服务健康状态
     */
    enum Health {
        /**
         * 服务正常
         */
        NORMAL,//
        /**
         * 不正常的服务，万不得已参与流量调度
         */
        ABNORMAL,
        /**
         * 不能参与流量调度
         */
        FORBIDDEN;
    }

    /**
     * 默认分片实现
     */
    class DefaultShard implements Shard {

        /**
         * The Name.
         */
        protected String name;
        /**
         * The Region.
         */
        protected String region;
        /**
         * The Data center.
         */
        protected String dataCenter;
        /**
         * The Protocol.
         */
        protected String protocol;
        /**
         * The Address.
         */
        protected URL url;
        /**
         * The Weight.
         */
        protected int weight;
        /**
         * The State.
         */
        protected ShardState state;
        /**
         * 计数器
         */
        protected LongAdder counter = new LongAdder();

        /**
         * 构造函数
         *
         * @param url
         */
        public DefaultShard(final URL url) {
            this(url.getAddress(),
                    url.getString(Environment.REGION),
                    url.getString(Environment.DATA_CENTER),
                    url.getProtocol(),
                    url,
                    url.getPositiveInt(WEIGHT),
                    ShardState.INITIAL);
        }

        /**
         * Instantiates a new Default shard.
         *
         * @param name       the name
         * @param dataCenter the data center
         * @param weight     the weight
         * @param region     the region
         * @param state      the state
         */
        public DefaultShard(String name, String dataCenter, String region, int weight, ShardState state) {
            this(name, region, dataCenter, null, null, weight, state);
        }

        /**
         * Instantiates a new Default shard.
         *
         * @param name       the name
         * @param region     the region
         * @param dataCenter the data center
         * @param protocol   the protocol
         * @param url        the address
         * @param weight     the weight
         * @param state      the state
         */
        public DefaultShard(String name, String region, String dataCenter, String protocol, URL url, int weight, ShardState state) {
            this.name = name;
            this.region = region;
            this.dataCenter = dataCenter;
            this.protocol = protocol;
            this.url = url;
            this.weight = weight;
            this.state = state;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getProtocol() {
            return protocol;
        }

        @Override
        public URL getUrl() {
            return url;
        }

        @Override
        public int getWeight() {
            return weight;
        }

        @Override
        public String getDataCenter() {
            return dataCenter;
        }

        @Override
        public String getRegion() {
            return region;
        }

        @Override
        public ShardState getState() {
            return state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DefaultShard that = (DefaultShard) o;

            if (weight != that.weight) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            if (region != null ? !region.equals(that.region) : that.region != null) {
                return false;
            }
            if (dataCenter != null ? !dataCenter.equals(that.dataCenter) : that.dataCenter != null) {
                return false;
            }
            if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null) {
                return false;
            }

            if (url != null ? !url.equals(that.url) : that.url != null) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
