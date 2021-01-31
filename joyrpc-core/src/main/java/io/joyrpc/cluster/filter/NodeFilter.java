package io.joyrpc.cluster.filter;

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

import io.joyrpc.cluster.Cluster;
import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extensible;

/**
 * 节点过滤
 */
@Extensible("NodeFilter")
public interface NodeFilter {

    /**
     * 节点是否过滤掉
     *
     * @param cluster 集群
     * @param node    节点
     * @return 过滤标识
     */
    boolean filter(Cluster cluster, Node node);
}
