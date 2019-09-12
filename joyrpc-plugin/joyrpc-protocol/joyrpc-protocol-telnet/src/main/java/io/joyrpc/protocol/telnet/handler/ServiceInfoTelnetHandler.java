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

import io.joyrpc.protocol.telnet.util.TelnetUtils;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.telnet.TelnetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;


/**
 * @date: 2019/1/22
 */
public class ServiceInfoTelnetHandler extends AbstractTelnetHandler {

    private final static Logger logger = LoggerFactory.getLogger(ServiceInfoTelnetHandler.class);

    @Override
    public String type() {
        return "info";
    }

    @Override
    public String description() {
        return "Usage:\tinfo [interface] [method]" + LINE
                + "Get the interface-service info. " + LINE +
                "If has [interface], show all methods info. Example: info [interface]." + LINE +
                "If has [interface] [method], show input method info. Example: info [interface] [methodName]" + LINE;
    }

    @Override
    public String shortDescription() {
        return "Get the interface-service info.";
    }

    @Override
    public TelnetResponse telnet(final Channel channel, final String[] args) {
        if (args == null || args.length == 0) {
            return new TelnetResponse(help());
        }
        String serviceName = args[0];
        if (serviceName == null) {
            logger.info("The service [" + args[0] + " ] is not exists!");
            return new TelnetResponse("{\"error\":\"This service is not exists!\"}");
        }
        Class clazz = null;
        try {
            clazz = Class.forName(args[0]);
        } catch (Exception e) {
            logger.error("", e);
            return new TelnetResponse("{\"error\":\"This service's class init error!\"}");
        }

        if (args.length > 1) {
            return new TelnetResponse(this.getMethodInfo(clazz, args[1]));
        }

        StringBuilder result = new StringBuilder();
        result.append("{");
        result.append("\"serviceName\":\"").append(clazz.getCanonicalName()).append("\",");
        Method methods[] = clazz.getDeclaredMethods();
        result.append("\"methods\":[");
        for (Method method : methods) {
            result.append("{\"methodName\":").append("\"").append(method.getName()).append("\",");
            Class exceptions[] = method.getExceptionTypes();
            if (exceptions.length > 0) {
                result.append("\"exceptions\":").append("\"");
                for (Class exception : exceptions) {
                    result.append(exception.getCanonicalName()).append(",");
                }
                result.delete(result.length() - 1, result.length());
                result.append("\",");
            }
            result.append("\"returnType\":");
            //返回值类型
            Type returnType = method.getGenericReturnType();
            if (returnType instanceof Class) {
                Class returnClass = (Class) returnType;
                result.append("\"").append(returnClass.getCanonicalName()).append("\"");
                if (returnClass.isArray()) {
                    Class comClass = returnClass.getComponentType();
                    while (comClass.isArray()) {
                        comClass = comClass.getComponentType();
                    }
                    TelnetUtils.scanParameter(comClass, new HashSet(), result);
                } else {
                    TelnetUtils.scanParameter(returnClass, new HashSet(), result);
                }
            } else if (returnType instanceof GenericArrayType) {
                String typeStr = returnType.toString();
                result.append(typeStr).append("\"");
                GenericArrayType compsType = (GenericArrayType) returnType;
                Type realType = compsType.getGenericComponentType();
                //直到不是数组类型时
                if (realType instanceof GenericArrayType) {
                    while (realType instanceof GenericArrayType) {
                        realType = ((GenericArrayType) realType).getGenericComponentType();
                    }
                }
                if (realType instanceof Class) {
                    Class realClass = (Class) realType;
                    TelnetUtils.scanParameter(realClass, new HashSet(), result);
                } else if (realType instanceof ParameterizedType) {
                    String realStr = realType.toString();
                    TelnetUtils.getGenericField(realStr, result, new HashSet());
                }
            } else {
                String typeStr = returnType.toString();
                result.append("\"").append(typeStr).append("\"");
                TelnetUtils.getGenericField(typeStr, result, new HashSet());
            }

            result.append(",\"parameters\":[");
            Type paramsType[] = method.getGenericParameterTypes();

            int i = 1;
            for (Type param : paramsType) {
                result.append("{\"param").append(i).append("\":\"");
                if (param instanceof Class) {
                    Class paramClass = (Class) param;
                    result.append(paramClass.getCanonicalName()).append("\"");
                    if (paramClass.isArray()) {
                        Class comClass = paramClass.getComponentType();
                        while (comClass.isArray()) {
                            comClass = comClass.getComponentType();
                        }
                        TelnetUtils.scanParameter(comClass, new HashSet(), result);
                    } else {
                        TelnetUtils.scanParameter(paramClass, new HashSet(), result);
                    }
                } else if (param instanceof GenericArrayType) {
                    String typeStr = param.toString();
                    result.append(typeStr).append("\"");
                    GenericArrayType compsType = (GenericArrayType) param;
                    Type realType = compsType.getGenericComponentType();
                    //直到不是数组类型时
                    if (realType instanceof GenericArrayType) {
                        while (realType instanceof GenericArrayType) {
                            realType = ((GenericArrayType) realType).getGenericComponentType();
                        }
                    }
                    if (realType instanceof Class) {
                        Class realClass = (Class) realType;
                        TelnetUtils.scanParameter(realClass, new HashSet(), result);
                    } else if (realType instanceof ParameterizedType) {
                        String realStr = realType.toString();
                        TelnetUtils.getGenericField(realStr, result, new HashSet());
                    }
                } else {
                    String typeStr = param.toString();
                    result.append(typeStr).append("\"");
                    TelnetUtils.getGenericField(typeStr, result, new HashSet());
                }
                result.append("},");
                i++;
            }
            if (i > 1) {
                result.delete(result.length() - 1, result.length());
            }
            result.append("]},");
        }
        result.delete(result.length() - 1, result.length());
        result.append("]}");
        return new TelnetResponse(result.toString());
    }


    private String getMethodInfo(Class clazz, String methodName) {
        Method methods[] = clazz.getDeclaredMethods();
        Method method = null;
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                method = m;
            }
        }
        if (method == null) {
            return "{\"error\":\"The method [" + methodName + "] is not exists!\"}";
        }
        StringBuilder result = new StringBuilder();
        result.append("{\"methodName\":\"").append(methodName).append("\",");
        Class exceptions[] = method.getExceptionTypes();
        if (exceptions.length > 0) {
            result.append("\"exceptions\":").append("\"");
            for (Class exception : exceptions) {
                result.append(exception.getCanonicalName()).append(",");
            }
            result.delete(result.length() - 1, result.length());
            result.append("\",");
        }
        result.append("\"returnType\":");
        //返回值类型
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof Class) {
            Class returnClass = (Class) returnType;
            result.append("\"").append(returnClass.getCanonicalName()).append("\"");
            if (returnClass.isArray()) {
                Class comClass = returnClass.getComponentType();
                while (comClass.isArray()) {
                    comClass = comClass.getComponentType();
                }
                TelnetUtils.scanParameter(comClass, new HashSet(), result);
            } else {
                TelnetUtils.scanParameter(returnClass, new HashSet(), result);
            }
        } else if (returnType instanceof GenericArrayType) {
            String typeStr = returnType.toString();
            result.append(typeStr).append("\"");
            GenericArrayType compsType = (GenericArrayType) returnType;
            Type realType = compsType.getGenericComponentType();
            //直到不是数组类型时
            if (realType instanceof GenericArrayType) {
                while (realType instanceof GenericArrayType) {
                    realType = ((GenericArrayType) realType).getGenericComponentType();
                }
            }
            if (realType instanceof Class) {
                Class realClass = (Class) realType;
                TelnetUtils.scanParameter(realClass, new HashSet(), result);
            } else if (realType instanceof ParameterizedType) {
                String realStr = realType.toString();
                TelnetUtils.getGenericField(realStr, result, new HashSet());
            }
        } else {
            String typeStr = returnType.toString();
            result.append("\"").append(typeStr).append("\"");
            TelnetUtils.getGenericField(typeStr, result, new HashSet());
        }

        result.append(",\"parameters\":[");
        Type paramsType[] = method.getGenericParameterTypes();

        int i = 1;
        for (Type param : paramsType) {
            result.append("{\"param").append(i).append("\":\"");
            if (param instanceof Class) {
                Class paramClass = (Class) param;
                result.append(paramClass.getCanonicalName()).append("\"");
                if (paramClass.isArray()) {
                    Class comClass = paramClass.getComponentType();
                    while (comClass.isArray()) {
                        comClass = comClass.getComponentType();
                    }
                    TelnetUtils.scanParameter(comClass, new HashSet(), result);
                } else {
                    TelnetUtils.scanParameter(paramClass, new HashSet(), result);
                }
            } else if (param instanceof GenericArrayType) {
                String typeStr = param.toString();
                result.append(typeStr).append("\"");
                GenericArrayType compsType = (GenericArrayType) param;
                Type realType = compsType.getGenericComponentType();
                //直到不是数组类型时
                if (realType instanceof GenericArrayType) {
                    while (realType instanceof GenericArrayType) {
                        realType = ((GenericArrayType) realType).getGenericComponentType();
                    }
                }
                if (realType instanceof Class) {
                    Class realClass = (Class) realType;
                    TelnetUtils.scanParameter(realClass, new HashSet(), result);
                } else if (realType instanceof ParameterizedType) {
                    String realStr = realType.toString();
                    TelnetUtils.getGenericField(realStr, result, new HashSet());
                }
            } else {
                String typeStr = param.toString();
                result.append(typeStr).append("\"");
                TelnetUtils.getGenericField(typeStr, result, new HashSet());
            }
            result.append("},");
            i++;
        }
        if (i > 1) {
            result.delete(result.length() - 1, result.length());
        }
        result.append("]}");
        return result.toString();

    }
}

