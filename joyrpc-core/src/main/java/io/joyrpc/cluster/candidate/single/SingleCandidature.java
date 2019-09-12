package io.joyrpc.cluster.candidate.single;

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
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

import java.util.ArrayList;
import java.util.List;

/**
 * 全部生效，适用于数据节点
 */
@Extension("single")
public class SingleCandidature implements Candidature {

    @Override
    public Result candidate(final URL url, final Candidate candidate) {
        List<Node> nodes = candidate.getNodes();
        int size = nodes.size();
        return new Result(size > 0 ? nodes.subList(0, 1) : new ArrayList<>(0),
                new ArrayList<>(0),
                size > 1 ? nodes.subList(1, nodes.size()) : new ArrayList<>(0),
                new ArrayList<>(0));
    }
}
