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

/**
 * 地域接口
 */
public interface Region {

    /**
     * 地域
     */
    String REGION = "region";
    /**
     * 数据中心（key值大小写敏感）
     */
    String DATA_CENTER = "datacenter";

    /**
     * 区域
     *
     * @return
     */
    String getRegion();

    /**
     * 数据中心
     *
     * @return
     */
    String getDataCenter();


    /**
     * Region默认实现
     */
    class DefaultRegion implements Region {
        /**
         * 区域
         */
        protected String region;
        /**
         * 数据中心
         */
        protected String dataCenter;

        /**
         * 构造函数
         *
         * @param region
         * @param dataCenter
         */
        public DefaultRegion(String region, String dataCenter) {
            this.region = region;
            this.dataCenter = dataCenter;
        }

        @Override
        public String getRegion() {
            return region;
        }

        @Override
        public String getDataCenter() {
            return dataCenter;
        }
    }
}
