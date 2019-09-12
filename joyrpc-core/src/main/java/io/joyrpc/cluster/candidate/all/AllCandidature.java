package io.joyrpc.cluster.candidate.all;

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
import io.joyrpc.cluster.candidate.Candidature;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

/**
 * 全部生效，适用于数据节点
 */
@Extension("all")
public class AllCandidature implements Candidature {

    @Override
    public Result candidate(final URL url, final Candidate candidate) {
        return new Result(candidate.getNodes());
    }
}
