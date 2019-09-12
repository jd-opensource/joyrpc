package io.joyrpc.exception;

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
 * 非法配置异常
 */
public class IllegalConfigureException extends InitializationException {

    /**
     * 错误的配置项，例如refernce.loadblance
     */
    private String configKey;

    /**
     * 错误的配置值，例如ramdom（正确的是random）
     */
    private String configValue;

    public IllegalConfigureException(String msg, String errorCode) {
        super(msg, errorCode);
    }

    /**
     * @param configKey
     * @param configValue
     */
    public IllegalConfigureException(String configKey, String configValue, String code) {
        super("The value of config " + configKey + " [" + configValue + "] is illegal, please check it", code);
        this.configKey = configKey;
        this.configValue = configValue;
    }

    /**
     * @param configKey
     * @param configValue
     * @param message
     */
    public IllegalConfigureException(String configKey, String configValue, String message, String code) {
        super("The value of config " + configKey + " [" + configValue + "] is illegal, " + message, code);
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

}
