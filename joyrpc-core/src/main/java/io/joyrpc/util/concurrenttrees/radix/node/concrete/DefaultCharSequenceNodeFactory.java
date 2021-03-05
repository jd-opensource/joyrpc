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
package io.joyrpc.util.concurrenttrees.radix.node.concrete;

import io.joyrpc.util.concurrenttrees.common.CharSequences;
import io.joyrpc.util.concurrenttrees.radix.node.Node;
import io.joyrpc.util.concurrenttrees.radix.node.NodeFactory;
import io.joyrpc.util.concurrenttrees.radix.node.concrete.charsequence.*;
import io.joyrpc.util.concurrenttrees.radix.node.concrete.voidvalue.VoidValue;
import io.joyrpc.util.concurrenttrees.radix.node.util.NodeUtil;

import java.util.List;

/**
 * A {@link NodeFactory} which creates various implementations of {@link Node} objects all of which store incoming
 * edge characters as a {@link CharSequence} (a <i>view</i> onto the original key) rather than copying the edge into a
 * character array.
 * <p/>
 * Returns an optimal node implementation depending on arguments supplied, which will be one of:
 * <ul>
 *     <li>{@link CharSequenceNodeDefault} - contains all possible fields</li>
 *     <li>{@link CharSequenceNodeNonLeafNullValue} - does not store a value, returns {@code null} for value</li>
 *     <li>{@link CharSequenceNodeNonLeafVoidValue} - does not store a value, returns {@link VoidValue} for value</li>
 *     <li>{@link CharSequenceNodeLeafVoidValue} - does not store child edges or a value, returns {@link VoidValue} for value</li>
 *     <li>{@link CharSequenceNodeLeafWithValue} - does not store child edges, but does store a value</li>
 * </ul>
 * <p/>
 * When the application supplies {@link VoidValue} for a value, this factory will omit actually storing that value
 * in the tree and will return one of the Void-optimized nodes above which can reduce memory usage.
 *
 * @author Niall Gallagher
 */
public class DefaultCharSequenceNodeFactory implements NodeFactory {

    @Override
    public Node createNode(CharSequence edgeCharacters, Object value, List<Node> childNodes, boolean isRoot) {
        if (edgeCharacters == null) {
            throw new IllegalStateException("The edgeCharacters argument was null");
        }
        if (!isRoot && edgeCharacters.length() == 0) {
            throw new IllegalStateException("Invalid edge characters for non-root node: " + CharSequences.toString(edgeCharacters));
        }
        if (childNodes == null) {
            throw new IllegalStateException("The childNodes argument was null");
        }
        NodeUtil.ensureNoDuplicateEdges(childNodes);


        if (childNodes.isEmpty()) {
            // Leaf node...
            if (value instanceof VoidValue) {
                return new CharSequenceNodeLeafVoidValue(edgeCharacters);
            } else if (value != null) {
                return new CharSequenceNodeLeafWithValue(edgeCharacters, value);
            } else {
                return new CharSequenceNodeLeafNullValue(edgeCharacters);
            }
        } else {
            // Non-leaf node...
            if (value instanceof VoidValue) {
                return new CharSequenceNodeNonLeafVoidValue(edgeCharacters, childNodes);
            } else if (value == null) {
                return new CharSequenceNodeNonLeafNullValue(edgeCharacters, childNodes);
            } else {
                return new CharSequenceNodeDefault(edgeCharacters, value, childNodes);
            }
        }
    }

}
