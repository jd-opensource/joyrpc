package io.joyrpc.cluster.candidate.region;

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

import io.joyrpc.cluster.Node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 数据中心分布
 */
public class DataCenterDistribution {
    protected String dataCenter;
    //本机房分片数量
    protected int size;
    //本机房首选分片
    protected LinkedList<Node> high = new LinkedList<>();
    //本机房未选择过的分片
    protected LinkedList<Node> normal = new LinkedList<>();
    //本机房不好的分片
    protected LinkedList<Node> low = new LinkedList<>();

    /**
     * 构造函数
     *
     * @param dataCenter
     */
    public DataCenterDistribution(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    /**
     * 添加节点
     *
     * @param node
     * @return
     */
    public boolean add(Node node) {
        size++;
        switch (node.getState()) {
            case CONNECTED:
            case CONNECTING:
            case WEAK:
            case CANDIDATE:
                return high.add(node);
            case INITIAL:
                return normal.add(node);
            default:
                return low.add(node);
        }
    }

    /**
     * 选择候选者
     *
     * @param candidates 选择的节点
     * @param backups    备份节点
     * @param source     原始节点
     * @param count      数量
     * @param sorter     排序器
     * @return
     */
    protected int candidate(final List<Node> candidates, final List<Node> backups, final List<Node> source,
                            final int count, final Consumer<List<Node>> sorter) {
        if (count <= 0) {
            backups.addAll(source);
            return 0;
        } else {
            int size = source.size();
            if (size <= count) {
                candidates.addAll(source);
                return size;
            } else {
                if (sorter != null) {
                    sorter.accept(source);
                }
                candidates.addAll(source.subList(0, count));
                backups.addAll(source.subList(count, size));
                return count;
            }
        }
    }

    /**
     * 选择候选者
     *
     * @param candidates 选择的节点
     * @param backups    冷备节点
     * @param count      数量
     * @return 添加的数量
     */
    public int candidate(final List<Node> candidates, final List<Node> backups, final int count) {
        int remain = count - candidate(candidates, backups, high, count, o -> o.sort(null));
        remain -= candidate(candidates, backups, normal, remain, o -> Collections.shuffle(o));
        remain -= candidate(candidates, backups, low, remain, o -> Collections.shuffle(o));
        return count - remain;
    }

    public int getSize() {
        return size;
    }
}
