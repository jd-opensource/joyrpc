package io.joyrpc.util.network;

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
 * IP分段
 */
public class IpPart {

    /**
     * 类型
     */
    protected IpType type;
    /**
     * 分段
     */
    protected int[] parts;

    public IpPart(IpType type, int[] parts) {
        this.type = type;
        this.parts = parts;
    }

    public IpType getType() {
        return type;
    }

    public int[] getParts() {
        return parts;
    }
}
