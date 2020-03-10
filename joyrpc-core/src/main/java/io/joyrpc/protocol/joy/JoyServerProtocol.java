package io.joyrpc.protocol.joy;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.AbstractProtocol;
import io.joyrpc.protocol.MsgType;
import io.joyrpc.protocol.ServerProtocol;
import io.joyrpc.protocol.joy.codec.JoyCodec;
import io.joyrpc.protocol.message.MessageHeader;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.message.Message;
import io.joyrpc.transport.session.Session;

import static io.joyrpc.protocol.Protocol.JOY_ORDER;
import static io.joyrpc.transport.session.Session.AUTH_SESSION_FAIL;
import static io.joyrpc.transport.session.Session.AUTH_SESSION_SUCCESS;

/**
 * Protocol<br>
 */
@Extension(value = "joy", order = JOY_ORDER)
public class JoyServerProtocol extends AbstractProtocol implements ServerProtocol {

    @Override
    protected Codec createCodec() {
        return new JoyCodec(this);
    }

    @Override
    public int authenticate(Session session) {
        return session.getAuthenticated();
    }

    @Override
    public Message offline(final URL url) {
        return new RequestMessage(new MessageHeader(MsgType.OfflineReq.getType()));
    }
}
