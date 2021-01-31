package io.joyrpc.cluster.candidate;

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
import io.joyrpc.extension.URL;

import java.util.LinkedList;
import java.util.List;


/**
 * 集群候选节点推荐逻辑
 */
@Extensible("candidature")
public interface Candidature {

    /**
     * Recommend candidate.
     *
     * @param url       URL
     * @param candidate 候选者
     */
    Result candidate(URL url, Candidate candidate);

    /**
     * 选择结果
     */
    class Result {

        //选择的节点
        protected List<Node> candidates;
        //热备的节点，权重为0
        protected List<Node> standbys;
        //冷备的节点
        protected List<Node> backups;
        //丢弃的节点
        protected List<Node> discards;

        public Result(List<Node> candidates) {
            this(candidates, null, null, null);
        }

        public Result(List<Node> candidates, List<Node> backups) {
            this(candidates, backups, null, null);
        }

        public Result(List<Node> candidates, List<Node> standbys, List<Node> backups, List<Node> discards) {
            this.candidates = candidates == null ? new LinkedList<>() : candidates;
            this.standbys = standbys == null ? new LinkedList<>() : standbys;
            this.backups = backups == null ? new LinkedList<>() : backups;
            this.discards = discards == null ? new LinkedList<>() : discards;
        }

        public List<Node> getCandidates() {
            return candidates;
        }

        public List<Node> getStandbys() {
            return standbys;
        }

        public List<Node> getBackups() {
            return backups;
        }

        public List<Node> getDiscards() {
            return discards;
        }

        /**
         * 建连的节点
         *
         * @return
         */
        public int getSize() {
            return candidates.size() + standbys.size();
        }
    }

}
