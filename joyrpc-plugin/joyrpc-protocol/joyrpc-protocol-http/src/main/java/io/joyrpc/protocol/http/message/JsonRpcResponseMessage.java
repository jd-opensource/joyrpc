package io.joyrpc.protocol.http.message;

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

import static io.joyrpc.Plugin.JSON;

/**
 * 应答
 */
public class JsonRpcResponseMessage extends AbstractJsonResponseMessage {
    /**
     * 版本
     */
    protected JsonRpcResponse jsonRpcResponse;

    public JsonRpcResponseMessage(JsonRpcResponse jsonRpcResponse) {
        this.jsonRpcResponse = jsonRpcResponse;
    }

    @Override
    protected void render() {
        if (!response.isError()) {
            jsonRpcResponse.setResult(response.getResponse());
        } else {
            jsonRpcResponse.setError(new JsonRpcError(-32603, response.getException().getMessage()));
        }
        content = JSON.get().toJSONBytes(jsonRpcResponse);
        status = 200;
    }
}
