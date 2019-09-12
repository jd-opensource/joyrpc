package io.joyrpc.event;

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
 * 发布者配置
 */
public class PublisherConfig {

    //队列容量
    protected int capacity;
    //入队默认超时时间
    protected long timeout;

    public PublisherConfig() {
    }

    public PublisherConfig(int capacity, long timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造器
     */
    public static class Builder {
        //队列容量
        protected int capacity;
        protected long timeout;

        public Builder capacity(int capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public PublisherConfig build() {
            return new PublisherConfig(capacity, timeout);
        }
    }

}
