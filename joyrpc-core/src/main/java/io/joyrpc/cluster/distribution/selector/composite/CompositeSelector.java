package io.joyrpc.cluster.distribution.selector.composite;

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
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.StringUtils;

import java.util.List;

import static io.joyrpc.Plugin.NODE_SELECTOR;

@Extension(value = CompositeSelector.COMPOSITE_SELECTOR)
public class CompositeSelector implements NodeSelector {

    public static final String COMPOSITE_SELECTOR = "compositeSelector";

    /**
     * URL配置
     */
    protected URL url;

    protected Class<?> interfaceClass;

    protected String className;

    protected NodeSelector[] selectors;

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setup() {
        String value = url.getString(Constants.COMPOSITE_SELECTOR_OPTION);
        if (value != null && !value.isEmpty()) {
            String[] parts = StringUtils.split(value, ',');
            for (int i = 0; i < parts.length; i++) {
                selectors[i] = NODE_SELECTOR.get(parts[i]);
                selectors[i].setUrl(url);
                selectors[i].setClass(interfaceClass);
                selectors[i].setClassName(className);
                selectors[i].setup();
            }
        }
    }

    @Override
    public List<Node> select(final Candidate candidate, final RequestMessage<Invocation> request) {
        if (selectors == null || selectors.length == 0) {
            return candidate.getNodes();
        }
        List<Node> result = null;
        NodeSelector selector;
        int count = 0;
        for (int i = 0; i < selectors.length; i++) {
            selector = selectors[i];
            if (selector != null) {
                result = selector.select(count == 0 ? candidate : new Candidate(candidate, result), request);
                count++;
            }
        }
        return result;
    }

    @Override
    public void setClass(Class clazz) {
        this.interfaceClass = clazz;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }
}
