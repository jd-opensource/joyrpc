package io.joyrpc.constants;

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
 * RPC 版本
 *
 */
public class Version {
    /**
     * 当前RPC版本，例如：<br>
     * 2.0.1-SNAPSHOT对应2010<br>
     * 2.0.1正式版对应2011
     */
    public static final short BUILD_VERSION = 2060;

    /**
     * 当前Build版本，每次发布修改
     */
    public static final String BUILD_VERSION_TIME = "2.0.6_202005090932";

    /**
     * 当前协议版本
     */
    public static final String PROTOCOL_VERSION = "joy1";

    /**
     * 当前的协议
     */
    public static final String PROTOCOL = "joy";

}
