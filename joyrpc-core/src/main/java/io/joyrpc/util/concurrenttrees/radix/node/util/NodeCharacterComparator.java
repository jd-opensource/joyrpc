/**
 * Copyright 2012-2013 Niall Gallagher
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.joyrpc.util.concurrenttrees.radix.node.util;

import java.util.Comparator;

/**
 * Specifies binary search compatibility, and sorting compatibility, of nodes based on
 * {@link io.joyrpc.util.concurrenttrees.radix.node.Node#getIncomingEdgeFirstCharacter()}.
 *
 * @author Niall Gallagher
 */
public class NodeCharacterComparator implements Comparator<NodeCharacterProvider> {

    @Override
    public int compare(NodeCharacterProvider o1, NodeCharacterProvider o2) {
        return o1.getIncomingEdgeFirstCharacter().compareTo(o2.getIncomingEdgeFirstCharacter());
    }
}
