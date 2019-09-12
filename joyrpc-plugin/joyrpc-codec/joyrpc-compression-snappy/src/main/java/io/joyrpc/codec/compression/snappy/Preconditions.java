package io.joyrpc.codec.compression.snappy;

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

/**
 * 检查
 */
public class Preconditions {

    protected Preconditions() {
    }

    /**
     * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in an array, list
     * or string of size {@code size}, and are in order. A position index may range from zero to
     * {@code size}, inclusive.
     *
     * @param start a user-supplied index identifying a starting position in an array, list or string
     * @param end   a user-supplied index identifying a ending position in an array, list or string
     * @param size  the size of that array, list or string
     * @throws IndexOutOfBoundsException if either index is negative or is greater than {@code size},
     *                                   or if {@code end} is less than {@code start}
     * @throws IllegalArgumentException  if {@code size} is negative
     */
    public static void checkPositionIndexes(final int start, final int end, final int size) {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (start < 0) {
            throw new IndexOutOfBoundsException(String.format("start index (%d) must not be negative", start));
        } else if (end < start) {
            throw new IndexOutOfBoundsException(String.format("end index (%d) must not be less than start index (%d)", end, start));
        } else if (end > size) {
            throw new IndexOutOfBoundsException(String.format("end index (%d) must not be greater than size (%d)", end, size));
        }
    }

}
