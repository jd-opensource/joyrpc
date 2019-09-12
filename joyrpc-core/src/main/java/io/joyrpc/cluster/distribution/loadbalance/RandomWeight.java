package io.joyrpc.cluster.distribution.loadbalance;

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

import io.joyrpc.cluster.Weighter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 加权随机负载均衡
 */
public class RandomWeight {

    /**
     * 随机选择
     *
     * @param nodes
     * @param <T>
     * @return
     */
    public static <T extends Weighter> T select(final List<T> nodes) {
        int size = nodes == null ? 0 : nodes.size();
        switch (size) {
            case 0:
                return null;
            case 1:
                return nodes.get(0);
            default:
                //总权重
                int totalWeight = 0;
                //一半权重，快速查找
                int halfWeight = 0;
                int half = size / 2;
                T last = null;
                int pos = 0;

                //权重
                for (T node : nodes) {
                    totalWeight += node.getWeight();
                    if (++pos == half) {
                        //一半权重
                        halfWeight = totalWeight;
                    }
                    last = node;
                }
                //权重和不大于零,直接退化为随机
                if (totalWeight <= 0) {
                    return nodes.get(ThreadLocalRandom.current().nextInt(nodes.size()));
                }

                //随机权重
                int random = ThreadLocalRandom.current().nextInt(totalWeight);
                //计算起始位置
                int start = random >= halfWeight ? half : 0;
                totalWeight = random >= halfWeight ? halfWeight : 0;
                //子集合
                List<T> subNodes = start == 0 ? nodes : nodes.subList(start, nodes.size());
                for (T node : subNodes) {
                    totalWeight += node.getWeight();
                    if (totalWeight >= random) {
                        return node;
                    }
                }
                return last;

        }
    }
}
