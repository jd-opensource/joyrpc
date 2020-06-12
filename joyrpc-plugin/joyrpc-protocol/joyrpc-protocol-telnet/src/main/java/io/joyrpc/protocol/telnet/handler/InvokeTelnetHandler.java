/**
 *
 */
package io.joyrpc.protocol.telnet.handler;

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

import io.joyrpc.Result;
import io.joyrpc.codec.Hex;
import io.joyrpc.codec.crypto.Encryptor;
import io.joyrpc.codec.crypto.Signature;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.ServiceManager;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import io.joyrpc.util.GenericMethod;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.network.Ipv4;
import io.joyrpc.util.network.Lan;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.Plugin.ENCRYPTOR;
import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.constants.Constants.ALIAS_EMPTY_OPTION;
import static io.joyrpc.context.Variable.VARIABLE;
import static io.joyrpc.util.StringUtils.isEmpty;

/**
 * Invoke命令
 */
public class InvokeTelnetHandler extends AbstractTelnetHandler {

    /**
     * slf4j Logger for this class
     */
    private final static Logger logger = LoggerFactory.getLogger(InvokeTelnetHandler.class);

    public InvokeTelnetHandler() {
        options = new Options()
                .addOption("g", true, "is global password")
                .addOption("a", "alias", true, "the alias of the service")
                .addOption("p", "password", true, "invoke -p password com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})")
                .addOption("t", "token", true, "invoke -p password -t token com.xxx.XxxService.xxxMethod(1234, \"abcd\", {\"prop\" : \"value\"})")
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command invoke");
    }

    @Override
    public String type() {
        return "invoke";
    }

    @Override
    public String description() {
        return "Invoke the specified method. (Before invoke need superuser's role)";
    }

    @Override
    public String shortDescription() {
        return "Invoke the specified method.";
    }

    @Override
    public String help() {
        return super.help("invoke [interface.method(args...)] ", options);
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        try {
            if (args == null || args.length == 0) {
                return new TelnetResponse(help());
            }
            CommandLine cmd = getCommand(options, args);
            //帮助命令
            if (cmd.hasOption(HELP_SHORT)) {
                return new TelnetResponse(help());
            }
            // 请求数据部分 com.xxx.XxxService.xxxMethod(args)
            String[] reqStrArgs = cmd.getArgs();
            String requestStr = reqStrArgs == null || reqStrArgs.length != 1 ? "" : reqStrArgs[0];
            //获取global
            boolean isGlobal = cmd.hasOption("g") ? true : false;
            //获取password
            String password = cmd.getOptionValue(isGlobal ? "g" : "p");
            //获取token
            String token = cmd.getOptionValue("t");
            //获取alias
            String alias = cmd.getOptionValue("a");
            // 解析接口和方法
            int i = requestStr.indexOf("(");
            if (i < 0 || !requestStr.endsWith(")")) {
                return new TelnetResponse("Invalid parameters, format: service.method(args)");
            }
            // 解析密码
            String interfaceId;
            String methodName;
            String serviceAndMethod = requestStr.substring(0, i).trim();
            String params = requestStr.substring(i + 1, requestStr.length() - 1).trim();
            i = serviceAndMethod.lastIndexOf(".");
            if (i >= 0) {
                interfaceId = serviceAndMethod.substring(0, i).trim();
                methodName = serviceAndMethod.substring(i + 1).trim();
            } else {
                return new TelnetResponse("Invalid parameters, format: service.method(args)");
            }
            //认证
            TelnetResponse validateRes = authenticate(interfaceId, password, isGlobal, channel);
            if (validateRes != null) {
                return validateRes;
            }

            Exporter exporter = getExporter(interfaceId, alias == null ? "" : alias);
            if (exporter == null) {
                return new TelnetResponse("Not found such exported service !");
            } else {
                return invoke(exporter, methodName, params, token, channel);
            }

        } catch (Exception e) {
            return new TelnetResponse(StringUtils.toString(e));
        }
    }

    /**
     * 调用
     * @param exporter 服务
     * @param methodName 方法名称
     * @param params 参数
     * @param token 令牌
     * @param channel 通道
     * @return 应答
     */
    protected TelnetResponse invoke(final Exporter exporter, final String methodName, final String params,
                                    final String token, final Channel channel) {
        StringBuilder buf = new StringBuilder();
        //查找类及找方法，不支持重载
        try {
            Invocation invocation = new Invocation(exporter.getInterfaceName(), exporter.getAlias(), methodName);
            RequestMessage<Invocation> request = RequestMessage.build(invocation, channel);
            invocation.addAttachment(Constants.INTERNAL_KEY_TELNET, true);
            if (token != null) {
                invocation.addAttachment(Constants.HIDDEN_KEY_TOKEN, token);
            }
            //设置
            exporter.setup(request);
            GenericMethod genericMethod = invocation.getGenericMethod();
            Type[] resolvedTypes = genericMethod.getGenericTypes();
            invocation.setGenericTypes(resolvedTypes);
            invocation.setArgsType(genericMethod.getTypes());
            invocation.setArgs(parse(params, resolvedTypes));

            long start = System.currentTimeMillis();
            CompletableFuture<Result> future = exporter.invoke(request);
            Result response = future.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            long end = System.currentTimeMillis();
            if (response.isException()) {
                Throwable e = response.getException();
                logger.error("error when telnet invoke", e);
                buf.append(StringUtils.toString(e));
            } else {
                buf.append(JSON.get().toJSONString(response.getValue()));
            }

            buf.append("\r\nelapsed: ");
            buf.append(end - start);
            buf.append(" ms.");
        } catch (SerializerException e) {
            buf.append("Invalid json argument, caused by: ").append(e.getMessage());
        } catch (Throwable t) {
            logger.error("error when telnet invoke", t);
            buf.append("Failed to invoke method ").append(methodName).append(", cause: ").append(StringUtils.toString(t));
        }
        return new TelnetResponse(buf.toString());
    }

    /**
     * 解析参数
     * @param text 文本
     * @param types 类型
     * @return 参数数组
     */
    protected Object[] parse(final String text, final Type[] types) {
        Object[] result = new Object[types.length];
        switch (types.length) {
            case 0:
                break;
            case 1:
                result[0] = JSON.get().parseObject(text, types[0]);
                break;
            default:
                String value = text;
                //解析多参数
                if (text.charAt(0) != '[' || text.charAt(text.length() - 1) != ']') {
                    //非数组
                    value = "[" + text + "]";
                }
                final int[] index = new int[]{0};
                JSON.get().parseArray(value, o -> {
                    result[index[0]] = o.apply(types[index[0]]);
                    return ++index[0] < types.length;
                });
        }
        return result;
    }

    /**
     * 获取Invoker对象
     * @param interfaceId
     * @param alias
     * @return
     */
    protected Exporter getExporter(final String interfaceId, final String alias) {
        Exporter exporter;
        if (alias != null && !alias.isEmpty() || alias != null && VARIABLE.getBoolean(ALIAS_EMPTY_OPTION)) {
            exporter = ServiceManager.getFirstExporter(interfaceId, alias);
        } else {
            exporter = ServiceManager.getFirstExporterByInterface(interfaceId);
        }
        return exporter;
    }

    /**
     * 验证调用权限
     * @param interfaceId 接口
     * @param password 密码
     * @param isGlobal 是否全局
     * @param channel 通道
     * @return 应答
     */
    protected TelnetResponse authenticate(final String interfaceId, final String password, final boolean isGlobal, final Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        String remoteIp = Ipv4.toIp(address);
        // 注册中心配的密码
        String token;
        if (!isGlobal) {
            //接口配置
            token = GlobalContext.get(interfaceId, Constants.SETTING_INVOKE_TOKEN, "");
            if (isEmpty(token)) {
                //全局配置
                token = VARIABLE.getString(Constants.SETTING_INVOKE_TOKEN);
            }
        } else {
            token = VARIABLE.getString(Constants.SETTING_SERVER_SUDO_PASSWD);
        }
        //获取加密算法
        String cryptoType = VARIABLE.getString(Constants.SETTING_SERVER_SUDO_CRYPTO, SUDO_CRYPTO_TYPE);
        //获取加密秘钥
        String cryptoKey = VARIABLE.getString(Constants.SETTING_SERVER_SUDO_CRYPTO_KEY);
        Encryptor encryptor = ENCRYPTOR.get(cryptoType);
        Lan lan = new Lan(VARIABLE.getString(Constants.SETTING_SERVER_SUDO_WHITELIST), true);
        // 此处验证密码 设置过sudo passwd可以调用
        Boolean sudo = channel.getAttribute(SUDO_ATTRIBUTE);
        // 本机地址可以直接调用
        if (Ipv4.isLocalIp(remoteIp)) {
            return null;
        } else if (!lan.contains(remoteIp)) {
            return new TelnetResponse("Failure, remote ip " + remoteIp + " is not in invoke whitelist");
        } else if (sudo != null && sudo) {
            //通过了sudo认证
            return null;
        } else if (isEmpty(password)) {
            return new TelnetResponse("Failure, password is null, please set it by \"invoke -p password \" or \"invoke -g password \"");
        } else if (isEmpty(token)) {
            // 没设置密码不让调用
            return new TelnetResponse("Failure, token is not configured.");
        } else if (encryptor == null) {
            return new TelnetResponse("Failure, encryptor is not found.");
        } else if (isEmpty(cryptoKey)) {
            return new TelnetResponse("Failure, cryptoKey is not configured.");
        } else {
            try {
                byte[] sources = password.getBytes(StandardCharsets.UTF_8);
                byte[] keys = Hex.decode(cryptoKey);
                byte[] signs = Hex.decode(token);
                boolean result = encryptor instanceof Signature ? ((Signature) encryptor).verify(sources, keys, signs)
                        : Arrays.equals(encryptor.encrypt(sources, keys), signs);
                if (!result) {
                    //校验不通过
                    return new TelnetResponse("Failure, wrong password!");
                }
                return null;
            } catch (Exception e) {
                return new TelnetResponse("Error occurs while verifing password, caused by:" + e.getMessage());
            }
        }
    }

}
