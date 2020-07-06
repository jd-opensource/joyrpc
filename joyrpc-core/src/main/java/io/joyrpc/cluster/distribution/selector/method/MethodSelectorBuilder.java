package io.joyrpc.cluster.distribution.selector.method;

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

import io.joyrpc.cluster.Shard;
import io.joyrpc.cluster.distribution.selector.method.predicate.*;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.joyrpc.Plugin.JSON;


/**
 * @ClassName: RouterConditionFactory
 * @Description: 路由条件工厂
 * @date 2019年3月5日 下午1:57:58
 */
public class MethodSelectorBuilder {

    protected final static String WHEN_FLAG_METHOD = "method";
    protected final static String WHEN_CONDITION_FLAG_IP = ".ip";
    protected final static String WHEN_CONDITION_FLAG_ARG = ".arg";
    protected final static String THEN_CONDITION_FLAG = "ipPattern";

    //when条件，判断本次请求是否需要路由
    protected final static Map<String, Function<String, BiPredicate<Shard, RequestMessage<Invocation>>>> WHEN_BUILDER = new HashMap<>();
    //then条件，如果请求需要路由，路由策略
    protected final static Map<String, Function<String, BiPredicate<Shard, RequestMessage<Invocation>>>> THEN_BUILDER = new HashMap<>();
    //when条件的配置信息格式
    protected static Pattern whenPattern = Pattern.compile("([^!=><]*)([!=><]{1,2})([^!=><]*)");

    static {
        //初始化when谓词条件
        //方法路由解析
        WHEN_BUILDER.put(WHEN_FLAG_METHOD, (s) -> {
            String[] groups = group(s);
            return groups == null ? null : new MethodNameMatcher(groups[2], Operator.of(groups[1]));
        });
        //IP路由解析
        WHEN_BUILDER.put(WHEN_CONDITION_FLAG_IP, (s) -> {
            String[] groups = group(s);
            if (groups != null) {
                BiPredicate<Shard, RequestMessage<Invocation>> ipMatch = new LocalIpMatcher(groups[2], Operator.of(groups[1]));
                //解析方法名匹配
                int index = groups[0].indexOf(WHEN_CONDITION_FLAG_IP);
                if (index > 0) {
                    String methodName = groups[0].substring(0, index);
                    ipMatch = new MethodNameMatcher(methodName, Operator.eq).and(ipMatch);
                }
                return ipMatch;
            }
            return null;
        });
        //参数路由解析
        WHEN_BUILDER.put(WHEN_CONDITION_FLAG_ARG, (s) -> {
            String[] groups = group(s);
            if (groups != null) {
                int argFlagIdx = groups[0].indexOf(WHEN_CONDITION_FLAG_ARG);
                int argIndexIdx = argFlagIdx + 4;
                int argIndex = 0;
                if (argIndexIdx >= 4 && argIndexIdx < groups[0].length()) {
                    argIndex = Integer.parseInt(groups[0].substring(argIndexIdx));
                }
                BiPredicate<Shard, RequestMessage<Invocation>> methodParameterMatch = new ParameterMatcher(argIndex, groups[2], Operator.of(groups[1]));
                if (argFlagIdx > 0) {
                    String methodName = groups[0].substring(0, argFlagIdx);
                    methodParameterMatch = new MethodNameMatcher(methodName, Operator.eq).and(methodParameterMatch);
                }
                return methodParameterMatch;
            }
            return null;
        });
        //初始化then谓词条件
        THEN_BUILDER.put(THEN_CONDITION_FLAG, (s) -> Optional.ofNullable(s).map(p -> new LanMatcher(p)).orElse(null));
    }

    /**
     * 获取分组信息
     *
     * @param condition
     * @return
     */
    protected static String[] group(final String condition) {
        String[] groups = null;
        Matcher matcher = whenPattern.matcher(condition);
        if (matcher.matches() && matcher.groupCount() == 3) {
            groups = new String[]{matcher.group(1), matcher.group(2), matcher.group(3)};
        }
        return groups;
    }

    /**
     * 构造条件路由规则
     *
     * @param json
     * @return
     */
    public static BiPredicate<Shard, RequestMessage<Invocation>> build(final String json) {
        BiPredicate<Shard, RequestMessage<Invocation>> predicate = null;
        if (json != null && !json.isEmpty()) {
            //json反序列化为Map
            Map<String, String> map = JSON.get().parseObject(json, Map.class);
            if (map != null && map.size() > 0) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    //遍历map的key生成条件when谓词
                    BiPredicate<Shard, RequestMessage<Invocation>> whenCond = buildWhen(entry.getKey());
                    //遍历map的value生成then谓词
                    BiPredicate<Shard, RequestMessage<Invocation>> thenCond = buildThen(entry.getValue());
                    if (thenCond != null) {
                        //when不匹配，或者when匹配then匹配，返回true
                        predicate = predicate == null ? whenCond.negate().or(thenCond) : predicate.and(whenCond.negate().or(thenCond));
                    }
                }
            }
        }
        return predicate;
    }

    /**
     * 条件表达式
     *
     * @param condition
     * @return
     */
    protected static BiPredicate<Shard, RequestMessage<Invocation>> buildWhen(final String condition) {
        if (condition.startsWith(WHEN_FLAG_METHOD)) {
            return WHEN_BUILDER.get(WHEN_FLAG_METHOD).apply(condition);
        } else if (condition.contains(WHEN_CONDITION_FLAG_IP)) {
            return WHEN_BUILDER.get(WHEN_CONDITION_FLAG_IP).apply(condition);
        } else if (condition.contains(WHEN_CONDITION_FLAG_ARG)) {
            return WHEN_BUILDER.get(WHEN_CONDITION_FLAG_ARG).apply(condition);
        } else {
            throw new InitializationException("Illegal route rule when: [" + condition + "]", ExceptionCode.CONSUMER_ROUTE_CONF);
        }
    }

    /**
     * 构造Then条件
     *
     * @param condition
     * @return
     */
    protected static BiPredicate<Shard, RequestMessage<Invocation>> buildThen(final String condition) {
        return condition == null || condition.isEmpty() ? null : THEN_BUILDER.get(THEN_CONDITION_FLAG).apply(condition);
    }

}
