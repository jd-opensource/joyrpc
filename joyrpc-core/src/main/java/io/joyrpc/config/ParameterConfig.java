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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.IllegalConfigureException;

/**
 * 自定义参数配置，可出现在registry，server，provider，consumer，method下面<br>
 */
public class ParameterConfig extends AbstractConfig {

    /**
     * 关键字
     */
    protected String key;

    /**
     * 值
     */
    protected String value;

    /**
     * 是否隐藏（是的话，业务代码不能获取到）
     */
    protected boolean hide = false;

    public String getKey() {
        return key;
    }

    /**
     * Sets key.
     *
     * @param key the key
     */
    public void setKey(String key) {
        if (!RequestContext.VALID_KEY.test(key)) {
            throw new IllegalConfigureException("param.key", key, "key can not start with "
                    + Constants.HIDE_KEY_PREFIX + " and " + Constants.INTERNAL_KEY_PREFIX, ExceptionCode.COMMON_ABUSE_HIDE_KEY);
        }
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

}
