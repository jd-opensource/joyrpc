package io.joyrpc.cluster.distribution.selector.tag;

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
import java.util.Objects;

import static io.joyrpc.constants.Constants.TAG_KEY_OPTION;

/**
 * 基于标签的节点选择器
 */
@Extension(value = TagSelector.TAG_SELECTOR)
public class TagSelector implements NodeSelector {

    public static final String TAG_SELECTOR = "tagSelector";

    /**
     * URL配置
     */
    protected URL url;
    /**
     * 标签key
     */
    protected String tagKey;
    /**
     * 配置的标签值
     */
    protected String tagValue;

    @Override
    public void setUrl(final URL url) {
        this.url = url;
    }

    @Override
    public void setup() {
        this.tagKey = url.getString(TAG_KEY_OPTION);
        this.tagValue = url.getString(tagKey);
    }

    @Override
    public List<Node> select(Candidate candidate, RequestMessage<Invocation> request) {
        String tag = request.getPayLoad().getAttachment(tagKey, tagValue);
        if (tag == null || tag.isEmpty()) {
            return candidate.getNodes();
        }
        List<Node> result = new LinkedList<>();
        List<Node> nodes = candidate.getNodes();
        URL url;
        //先遍历服务列表
        for (Node node : nodes) {
            url = node.getUrl();
            String nodeTag = url == null ? null : url.getString(tagKey);
            if (Objects.equals(tag, nodeTag)) {
                result.add(node);
            }
        }
        return result;
    }
}
