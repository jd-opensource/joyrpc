package io.joyrpc.cluster.distribution;

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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.extension.Extensible;

import java.util.List;

/**
 * 重试目标节点选择器
 */
@Extensible("retrySelector")
public interface FailoverSelector {

    /**
     * 选择重试的目标节点
     *
     * @param candidate 候选者
     * @param node      当前节点
     * @param retry     当前重试次数
     * @param fails     失败节点
     * @param origins   原始节点
     * @return 候选者
     */
    Candidate select(Candidate candidate, Node node, int retry, List<Node> fails, List<Node> origins);

}
