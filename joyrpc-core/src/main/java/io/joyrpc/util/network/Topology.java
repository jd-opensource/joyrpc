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

import java.util.List;

/**
 * 网络拓扑图
 */
public class Topology<T extends Lan> {
    // 数据中心
    private List<T> lans;
    // 专线
    private List<Line<T>> lines;

    public Topology(List<T> lans, List<Line<T>> lines) {
        this.lans = lans;
        this.lines = lines;
    }

    public List<T> getLans() {
        return lans;
    }

    public List<Line<T>> getLines() {
        return lines;
    }
}
