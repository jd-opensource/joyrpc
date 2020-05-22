package io.joyrpc.spring;

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

import io.joyrpc.config.AbstractConfig;
import io.joyrpc.context.GlobalContext;
import org.springframework.beans.factory.InitializingBean;

import static io.joyrpc.constants.Constants.HIDE_KEY_PREFIX;

/**
 * 全局参数
 *
 * @description:
 */
public class GlobalParameterBean extends AbstractConfig implements InitializingBean {

    /**
     * 关键字
     */
    protected String key;

    /**
     * 值
     */
    protected Object value;
    /**
     * 是否隐藏（是的话，业务代码不能获取到）
     */
    protected boolean hide = false;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    @Override
    public void afterPropertiesSet() {
        validate();
        if (key != null && !key.isEmpty() && value != null) {
            if (hide) {
                if (key.charAt(0) != HIDE_KEY_PREFIX) {
                    GlobalContext.putIfAbsent(HIDE_KEY_PREFIX + key, value);
                } else {
                    GlobalContext.putIfAbsent(key, value);
                }
            } else {
                GlobalContext.putIfAbsent(key, value);
            }
        }
    }
}
