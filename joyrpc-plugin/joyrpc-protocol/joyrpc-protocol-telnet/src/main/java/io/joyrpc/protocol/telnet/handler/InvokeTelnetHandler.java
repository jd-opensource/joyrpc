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
import io.joyrpc.codec.crypto.Encryptor;
import io.joyrpc.constants.Constants;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.invoker.Exporter;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.network.Ipv4;
import io.joyrpc.util.network.Lan;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static io.joyrpc.Plugin.ENCRYPTOR;
import static io.joyrpc.Plugin.JSON;
import static io.joyrpc.codec.Hex.encode;
import static io.joyrpc.util.ClassUtils.getPublicMethod;

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
                .addOption("g", true, "is globle password")
                .addOption("a", "alias", false, "the alias of the service")
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

            Exporter exporter = getExporter(interfaceId, alias);
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
     * @param exporter
     * @param methodName
     * @param params
     * @param token
     * @param channel
     * @return
     */
    protected TelnetResponse invoke(final Exporter exporter, final String methodName, final String params,
                                    final String token, final Channel channel) {
        StringBuilder buf = new StringBuilder();
        //查找类及找方法，不支持重载
        try {
            Method method = getPublicMethod(exporter.getInterfaceClass(), methodName);
            Parameter[] parameters = method.getParameters();
            Object[] paramArgs = new Object[parameters.length];
            switch (parameters.length) {
                case 0:
                    break;
                case 1:
                    paramArgs[0] = JSON.get().parseObject(params, parameters[0].getParameterizedType());
                    break;
                default:
                    String value = params;
                    //解析多参数
                    if (params.charAt(0) != '[' || params.charAt(params.length() - 1) != ']') {
                        //非数组
                        value = "[" + params + "]";
                    }
                    final int[] index = new int[]{0};
                    JSON.get().parseArray(value, o -> {
                        paramArgs[index[0]] = o.apply(parameters[index[0]].getParameterizedType());
                        return ++index[0] < parameters.length;
                    });
            }
            long start = System.currentTimeMillis();
            RequestMessage<Invocation> request = RequestMessage.build(new Invocation(exporter.getInterfaceClass(), method, paramArgs), channel);
            Invocation invocation = request.getPayLoad();
            invocation.addAttachment(".telnet", true);
            if (token != null) {
                invocation.addAttachment(Constants.HIDDEN_KEY_TOKEN, token);
            }
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
        } catch (NoSuchMethodException e) {
            buf.append("No such method " + methodName + " in interface " + exporter.getInterfaceName());
        } catch (MethodOverloadException e) {
            buf.append("Overload method " + methodName + " in interface " + exporter.getInterfaceName());
        } catch (SerializerException e) {
            buf.append("Invalid json argument, caused by: ").append(e.getMessage());
        } catch (Throwable t) {
            logger.error("error when telnet invoke", t);
            buf.append("Failed to invoke method ").append(methodName).append(", cause: ").append(StringUtils.toString(t));
        }
        return new TelnetResponse(buf.toString());
    }

    /**
     * 获取Invoker对象
     * @param interfaceId
     * @param alias
     * @return
     */
    protected Exporter getExporter(final String interfaceId, final String alias) {
        return alias != null ? InvokerManager.getFirstExporter(interfaceId, alias) : InvokerManager.getFirstExporterByInterface(interfaceId);
    }

    /**
     * 验证调用权限
     * @param interfaceId
     * @param password
     * @param isGlobal
     * @param channel
     * @return
     */
    protected TelnetResponse authenticate(final String interfaceId, final String password, final boolean isGlobal, final Channel channel) {
        InetSocketAddress address = channel.getRemoteAddress();
        String remoteIp = Ipv4.toIp(address);
        Parametric parametric = new MapParametric(GlobalContext.getGlobalSetting());
        // 注册中心配的密码
        String invokePassword = !isGlobal ? GlobalContext.get(interfaceId, Constants.SETTING_INVOKE_TOKEN, "")
                : parametric.getString(Constants.SETTING_SERVER_SUDO_PASSWD, "");
        Lan lan = new Lan(parametric.getString(Constants.SETTING_SERVER_SUDO_WHITELIST), true);
        // 此处验证密码 设置过sudo passwd可以调用
        Boolean sudo = channel.getAttribute(SUDO_ATTRIBUTE);
        // 本机地址可以直接调用
        if (Ipv4.isLocalIp(remoteIp)) {
            return null;
        } else if (!lan.contains(remoteIp)) {
            return new TelnetResponse("Remote ip " + remoteIp + " is not in invoke whitelist");
        } else if (sudo != null && sudo) {
            return null;
        } else if (password == null) {
            return new TelnetResponse("Password is null, please set it by \"invoke -p password \"");
        } else if (invokePassword == null) { // 没设置密码不让调用
            return new TelnetResponse("please set password by administrator website.");
        } else {
            try {
                //获取加密算法
                String cryptoType = parametric.getString(Constants.SETTING_SERVER_SUDO_CRYPTO, SUDO_CRYPTO_TYPE);
                Encryptor encryptor = ENCRYPTOR.get(cryptoType);
                //获取加密秘钥
                String cryptoKey = parametric.getString(Constants.SETTING_SERVER_SUDO_CRYPTO_KEY, "");
                byte[] cryptoKeyBytes = StringUtils.isEmpty(cryptoKey) ? DEFAULT_CRYPTO_KEY : cryptoKey.getBytes();
                //校验
                if (!invokePassword.equals(encode(encryptor.encrypt(password.getBytes(), cryptoKeyBytes)))) {
                    return new TelnetResponse("Wrong password [" + password + "], please check it");
                }
                return null;
            } catch (Exception e) {
                return new TelnetResponse("Error occurs while verifing password, caused by:" + e.getMessage());
            }
        }
    }

    private static final byte[] DEFAULT_CRYPTO_KEY = new byte[]{-84, -19, 0, 5, 115, 114, 0,
            20, 106, 97, 118, 97, 46, 115, 101, 99, 117, 114, 105,
            116, 121, 46, 75, 101, 121, 82, 101, 112, -67, -7, 79,
            -77, -120, -102, -91, 67, 2, 0, 4, 76, 0, 9, 97, 108,
            103, 111, 114, 105, 116, 104, 109, 116, 0, 18, 76, 106,
            97, 118, 97, 47, 108, 97, 110, 103, 47, 83, 116, 114,
            105, 110, 103, 59, 91, 0, 7, 101, 110, 99, 111, 100,
            101, 100, 116, 0, 2, 91, 66, 76, 0, 6, 102, 111, 114,
            109, 97, 116, 113, 0, 126, 0, 1, 76, 0, 4, 116, 121,
            112, 101, 116, 0, 27, 76, 106, 97, 118, 97, 47, 115,
            101, 99, 117, 114, 105, 116, 121, 47, 75, 101, 121, 82,
            101, 112, 36, 84, 121, 112, 101, 59, 120, 112, 116, 0,
            6, 68, 69, 83, 101, 100, 101, 117, 114, 0, 2, 91, 66,
            -84, -13, 23, -8, 6, 8, 84, -32, 2, 0, 0, 120, 112, 0,
            0, 0, 24, -15, 61, 52, 26, 38, 109, 67, -62, 59, 31,
            42, 62, 49, -105, -2, -50, 25, 121, 62, -29, 52, -70,
            -15, -56, 116, 0, 3, 82, 65, 87, 126, 114, 0, 25, 106,
            97, 118, 97, 46, 115, 101, 99, 117, 114, 105, 116, 121,
            46, 75, 101, 121, 82, 101, 112, 36, 84, 121, 112, 101,
            0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 120, 114, 0, 14, 106,
            97, 118, 97, 46, 108, 97, 110, 103, 46, 69, 110, 117,
            109, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 120, 112, 116,
            0, 6, 83, 69, 67, 82, 69, 84};

}
