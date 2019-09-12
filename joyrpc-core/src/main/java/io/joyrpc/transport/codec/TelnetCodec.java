package io.joyrpc.transport.codec;

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

import io.joyrpc.exception.CodecException;
import io.joyrpc.transport.buffer.ChannelBuffer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetEscape;
import io.joyrpc.transport.telnet.TelnetInput;
import io.joyrpc.transport.telnet.TelnetRequest;
import io.joyrpc.transport.telnet.TelnetResponse;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @date: 2019/4/28
 */
public class TelnetCodec implements Codec {

    protected static final String TELNET_INPUT = "TELNET_INPUT";
    protected static final String CHARSET = "charset";
    protected static final String PROMPT = ">";

    protected String prompt = PROMPT;

    public TelnetCodec() {
    }

    public TelnetCodec(final String prompt) {
        this.prompt = prompt;
    }

    @Override
    public Object decode(final DecodeContext context, final ChannelBuffer buffer) throws CodecException {
        Channel channel = context.getChannel();
        int length = buffer.readableBytes();
        if (length == 0) {
            return null;
        }
        byte bytes[] = new byte[length];
        buffer.readBytes(bytes);
        if (TelnetInput.isBackspace(bytes)) {
            return onBackspace(channel, buffer, bytes);
        } else if (TelnetInput.isUp(bytes)) {
            return onUp(channel, buffer, bytes);
        } else if (TelnetInput.isDown(bytes)) {
            return onDown(channel, buffer, bytes);
        } else if (TelnetInput.isExit(bytes)) {
            return onExit(channel, buffer, bytes);
        } else if (TelnetInput.isEnter(bytes)) {
            return onEnter(channel, buffer, bytes);
        } else if (bytes[0] == TelnetEscape.NVT_IAC) {
            return onNvt(channel, buffer, bytes);
        } else {
            return onOther(channel, buffer, bytes);
        }
    }

    /**
     * 其它
     *
     * @param channel
     * @param bytes
     * @return
     */
    protected Object onOther(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        // 追加到当前缓冲器
        Charset charset = getCharset(channel);
        TelnetInput input = getTelnetInput(channel);
        input.append(new String(bytes, charset));
        return null;
    }

    /**
     * 获取输入
     *
     * @param channel
     * @return
     */
    protected TelnetInput getTelnetInput(Channel channel) {
        return channel.getAttribute(TELNET_INPUT, o -> new TelnetInput(prompt, 70));
    }

    /**
     * 获取字符集
     *
     * @param channel
     * @return
     */
    protected Charset getCharset(final Channel channel) {
        return channel.getAttribute(CHARSET, o -> StandardCharsets.UTF_8);
    }

    /**
     * 回车键
     *
     * @param channel
     * @param buffer
     * @param bytes
     * @return
     */
    protected Object onEnter(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        Charset charset = getCharset(channel);
        TelnetInput input = getTelnetInput(channel);
        // 回车
        String temp = new String(bytes, charset).trim();
        if (!temp.isEmpty()) {
            input.append(temp);
        }
        if (input.isEmpty()) {
            return new TelnetResponse(prompt);
        } else {
            String cmd = input.getInput().trim();
            input.delete();
            input.addHistory(cmd);
            return TelnetRequest.parse(cmd, prompt);
        }
    }

    /**
     * 控制命令
     *
     * @param channel
     * @param buffer
     * @param bytes
     * @return
     */
    protected Object onNvt(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        // TODO 控制命令，暂时忽略
        return null;
    }

    /**
     * 退出
     *
     * @param channel
     * @param buffer
     * @param bytes
     * @return
     */
    protected Object onExit(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        //退出操作
        channel.close();
        return null;
    }

    /**
     * 向下翻
     *
     * @param channel
     * @param buffer
     * @param bytes
     */
    protected Object onDown(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        TelnetInput input = getTelnetInput(channel);
        //下键翻阅历史命令
        String downCmd = input.roll(false);
        StringBuilder sb = new StringBuilder();
        if (downCmd != null) {
            for (int i = 0; i <= bytes.length; i++) {
                sb.append("\b");
            }
            for (int i = 0; i <= bytes.length; i++) {
                sb.append(" ");
            }
            for (int i = 0; i <= bytes.length; i++) {
                sb.append("\b");
            }
            sb.append(downCmd.trim().substring(3));
        }
        return new TelnetResponse(sb.toString());
    }

    /**
     * 向上翻
     *
     * @param channel
     * @param buffer
     * @param bytes
     * @return
     */
    protected Object onUp(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        TelnetInput input = getTelnetInput(channel);
        //上键翻阅历史命令
        String upCmd = input.roll(true);
        StringBuilder sb = new StringBuilder();
        if (upCmd != null) {
            for (int i = 0; i <= bytes.length; i++) {
                sb.append("\b");
            }
            for (int i = 0; i <= bytes.length; i++) {
                sb.append(" ");
            }
            for (int i = 0; i <= bytes.length; i++) {
                sb.append("\b");
            }
            sb.append(upCmd.trim().substring(3));
        }
        return new TelnetResponse(sb.toString());
    }

    /**
     * 退格
     *
     * @param channel
     * @param buffer
     * @param bytes
     * @return
     */
    protected Object onBackspace(final Channel channel, final ChannelBuffer buffer, final byte[] bytes) {
        TelnetInput input = getTelnetInput(channel);
        String text;
        if (input.isEmpty()) {
            text = ">";
        } else {
            char ch = input.deleteLast();
            if (TelnetInput.isDoubleByteChar(ch)) {
                text = "\b  \b\b";
            } else {
                text = new String(new byte[]{32, 8});
            }
        }
        return new TelnetResponse(text);
    }

    @Override
    public void encode(final EncodeContext context, final ChannelBuffer buffer, final Object message) {
        if (message == null) {
            return;
        }
        Charset charset = getCharset(context.getChannel());
        buffer.writeBytes(message.toString().getBytes(charset));
    }
}
