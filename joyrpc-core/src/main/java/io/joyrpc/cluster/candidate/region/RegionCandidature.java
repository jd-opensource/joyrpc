package io.joyrpc.cluster.candidate.region;

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
 * 区域感知算法，优先本地机房，再考虑本区域
 */
@Extension("region")
public class RegionCandidature implements Candidature {

    @Override
    public Result candidate(final URL url, final Candidate candidate) {
        if (candidate == null) {
            return null;
        }

        RegionDistribution region = new RegionDistribution(candidate.getRegion(), candidate.getDataCenter(), candidate.getNodes(), url);
        return region.candidate(candidate.getSize());
    }

}
