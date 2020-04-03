package io.joyrpc.cluster.discovery.backup;

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

import io.joyrpc.extension.Extensible;

import java.io.IOException;

/**
 * 备份恢复
 */
@Extensible("backup")
public interface Backup {

    /**
     * 恢复备份数据
     *
     * @param name 名称
     * @return 备份数据
     * @throws IOException io异常
     */
    BackupDatum restore(String name) throws IOException;

    /**
     * 备份数据
     *
     * @param name  名称
     * @param datum 备份数据
     * @throws IOException io异常
     */
    void backup(String name, BackupDatum datum) throws IOException;

}
