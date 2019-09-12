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

import io.joyrpc.constants.Constants;
import io.joyrpc.constants.Version;
import io.joyrpc.extension.URL;
import io.joyrpc.invoker.InvokerManager;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @date: 2019/1/22
 */
public class ListTelnetHandler extends AbstractTelnetHandler {

    public ListTelnetHandler() {
        options = new Options()
                .addOption("l", "if no has args, show interfaces detail, else show methods detail.")
                .addOption(HELP_SHORT, HELP_LONG, false, "show help message for command ll.");
    }

    @Override
    public String description() {
        return "Display all service interface and included methods.";
    }

    @Override
    public String type() {
        return "ls";
    }

    @Override
    public TelnetResponse telnet(Channel channel, String[] args) {
        CommandLine cmd = getCommand(options, args);
        if (cmd.hasOption(HELP_SHORT)) {
            return new TelnetResponse(help());
        } else {
            StringBuilder builder = new StringBuilder(1024);
            //获取参数
            String[] realArgs = cmd.getArgs();
            if (realArgs == null || realArgs.length == 0) {
                Set<ProviderInfo> providers = new HashSet<>();
                InvokerManager.exports(o -> providers.add(new ProviderInfo(o.getInterfaceClass().getCanonicalName(), o.getInterfaceClass(), o.getUrl())));
                //打印所有接口名称
                if (cmd.hasOption("l")) {
                    providers.forEach(p -> builder.append(p.getIfaceName()).append(" -> ")
                            .append(p.getUrl().toString(false, true, Constants.ALIAS_OPTION.getName(), Constants.BUILD_VERSION_KEY))
                            .append(LINE));
                } else {
                    providers.forEach(p -> builder.append(p.getIfaceName()).append(LINE));
                }
            } else {
                Map<String, ProviderInfo> providerMap = new HashMap<>();
                InvokerManager.exports(o -> {
                    ProviderInfo providerInfo = new ProviderInfo(o.getInterfaceClass().getCanonicalName(), o.getInterfaceClass(), o.getUrl());
                    providerMap.put(providerInfo.getIfaceName(), providerInfo);
                });
                boolean detail = cmd.hasOption("l");
                Class clzz;
                //变量接口
                for (String arg : cmd.getArgList()) {
                    ProviderInfo providerInfo = providerMap.get(arg);
                    clzz = providerInfo == null ? null : providerInfo.getInfaceClazz();
                    //判断是否输出
                    if (clzz != null) {
                        //找到类
                        describe(clzz, detail, builder);
                    } else {
                        builder.append("interface is not export. ").append(arg).append(LINE);
                    }
                }
            }
            return new TelnetResponse(builder.toString());
        }


    }

    /**
     * 描述方法信息
     * @param clazz
     * @param detail
     * @param builder
     */
    protected void describe(final Class clazz, final boolean detail, final StringBuilder builder) {
        builder.append(clazz.getCanonicalName()).append(LINE);
        //打印方法
        for (Method method : clazz.getMethods()) {
            if (!detail) {
                //打印方法
                builder.append(method.getName()).append(LINE);
            } else {
                //详细打印，打印返回值和参数类型
                builder.append(describeReturnType(method)).append(' ').append(method.getName()).append('(');
                describeParameters(method, builder);
                builder.append(')');
                describeExceptions(method, builder);
                builder.append(LINE);
            }
        }
    }

    /**
     * 描述方法参数类型
     * @param m
     * @param builder
     */
    protected void describeParameters(final Method m, final StringBuilder builder) {
        Parameter[] parameters;
        parameters = m.getParameters();
        if (parameters.length > 0) {
            for (Parameter p : parameters) {
                builder.append(describeParameterType(p));
                if (p.isNamePresent()) {
                    builder.append(' ').append(p.getName());
                }
                builder.append(", ");
            }
            builder.delete(builder.length() - 2, builder.length());
        }
    }

    /**
     * 描述异常信息
     * @param method
     * @param builder
     */
    protected void describeExceptions(final Method method, final StringBuilder builder) {
        Class[] exceptions;
        exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            builder.append(" throws");
            for (Class e : exceptions) {
                builder.append(' ').append(e.getCanonicalName());
            }
        }
    }

    /**
     * 描述参数类型
     * @param p
     * @return
     */
    protected String describeParameterType(final Parameter p) {
        return describeType(p.getParameterizedType());
    }

    /**
     * 描述返回类型
     * @param method
     * @return
     */
    protected String describeReturnType(final Method method) {
        return describeType(method.getGenericReturnType());
    }

    /**
     * 描述类型信息
     * @param type
     * @return
     */
    protected String describeType(final Type type) {
        if (type instanceof Class) {
            return ((Class) type).getCanonicalName();
        }
        return type.toString();
    }

    private static class ProviderInfo {

        private String ifaceName;
        private Class infaceClazz;

        private URL url;

        public ProviderInfo(String ifaceName, Class infaceClazz, URL url) {
            this.ifaceName = ifaceName;
            this.infaceClazz = infaceClazz;
            this.url = url.addIfAbsent(Constants.BUILD_VERSION_KEY, Version.BUILD_VERSION);
        }

        public String getIfaceName() {
            return ifaceName;
        }

        public void setIfaceName(String ifaceName) {
            this.ifaceName = ifaceName;
        }

        public Class getInfaceClazz() {
            return infaceClazz;
        }

        public void setInfaceClazz(Class infaceClazz) {
            this.infaceClazz = infaceClazz;
        }

        public URL getUrl() {
            return url;
        }

        public void setUrl(URL url) {
            this.url = url;
        }
    }

    @Override
    public boolean newLine() {
        return false;
    }

}
