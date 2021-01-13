package io.joyrpc.cluster.distribution.selector.simple;

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
import io.joyrpc.cluster.distribution.NodeSelector;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.joyrpc.constants.Constants.SIMPLE_SELECTOR_OPTION;

@Extension(value = SimpleSelector.SIMPLE_SELECTOR)
public class SimpleSelector implements NodeSelector {

    public static final String SIMPLE_SELECTOR = "simpleSelector";

    /**
     * URL配置
     */
    protected URL url;
    /**
     * 标签key
     */
    protected int simple;

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setup() {
        this.simple = url.getPositiveInt(SIMPLE_SELECTOR_OPTION);
    }

    @Override
    public List<Node> select(final Candidate candidate, final RequestMessage<Invocation> request) {
        List<Node> nodes = candidate.getNodes();
        if (nodes.size() <= simple) {
            return nodes;
        }
        List<Node> result = new LinkedList<>();
        int rnd = ThreadLocalRandom.current().nextInt(nodes.size());
        int max = Math.min(rnd + simple, nodes.size());
        for (int i = rnd; i <= max; i++) {
            result.add(nodes.get(i));
        }
        if (nodes.size() < simple) {
            max = simple - nodes.size();
            for (int i = 0; i <= max; i++) {
                result.add(nodes.get(i));
            }
        }
        return result;
    }
}
