package io.joyrpc.config;

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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象ID配置
 */
public abstract class AbstractIdConfig extends AbstractConfig {

    /**
     * Id生成器
     */
    protected final static AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    /**
     * Spring的BeanId
     */
    protected String id;

    public AbstractIdConfig() {
    }

    public AbstractIdConfig(AbstractIdConfig config) {
        super(config);
        //不复制ID，产生新的ID
        getId();
    }

    public String getId() {
        if (id == null) {
            synchronized (this) {
                if (id == null) {
                    id = "joy-cfg-gen-" + ID_GENERATOR.getAndIncrement();
                }
            }
        }
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
