package io.joyrpc.invoker.parameter;

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
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.context.IntfConfiguration;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.NoReferException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URLOption;
import io.joyrpc.invoker.AbstractGroupInvoker;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.GenericMethodOption;
import io.joyrpc.util.Shutdown;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.DST_PARAM_OPTION;
import static io.joyrpc.constants.Constants.METHOD_KEY_FUNC;
import static io.joyrpc.context.router.GroupRouterConfiguration.GROUP_ROUTER;

/**
 * 参数分组路由
 */
@Extension("parameter")
public class ParameterGroupInvoker extends AbstractGroupInvoker {

    /**
     * 方法分组参数
     */
    protected GenericMethodOption<MethodGroup> options;
    /**
     * 接口级别默认路由参数位置
     */
    protected Integer parameter;
    /**
     * 分组参数路由配置
     */
    protected IntfConfiguration<String, Map<String, Map<String, String>>> groupConfig;

    @Override
    public void setup() {
        super.setup();
        parameter = url.getNaturalInt(DST_PARAM_OPTION);
        options = new GenericMethodOption<>(clazz, className, method -> {
            //先从GROUP_ROUTER拿到初始化配置，这个时候groupConfig可能还没有创建
            //泛型调用的情况下，groupConfig可能更新不了，需要初始化就赋值
            Map<String, Map<String, String>> groups = GROUP_ROUTER.get(className);
            Map<String, String> methodGroups = groups == null ? null : groups.get(method);
            return new MethodGroup(
                    url.getPositiveInt(
                            new URLOption<>(METHOD_KEY_FUNC.apply(method, DST_PARAM_OPTION.getName()), parameter)),
                    methodGroups);
        });
        //分组路由配置监听器
        groupConfig = new IntfConfiguration<>(GROUP_ROUTER, className, config ->
                options.forEach((method, mg) -> {
                    //方法的参数路由配置
                    mg.groups = config == null ? null : config.get(method);
                    mg.defGroup = mg.groups == null ? null : mg.groups.get("*");
                }));

    }

    @Override
    public CompletableFuture<Void> close(final boolean gracefully) {
        //关闭监听器
        if (groupConfig != null) {
            groupConfig.close();
        }
        return super.close(gracefully);
    }

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        //选择分组
        String alias = router(request);
        //找到分组配置
        ConsumerConfig<?> config = configMap.get(alias);
        if (config != null) {
            //调用
            return config.getRefer().invoke(request);
        } else if (Shutdown.isShutdown()) {
            return CompletableFuture.completedFuture(new Result(request.getContext(), new ShutdownExecption("Refer is shutdown.", false)));
        } else {
            CompletableFuture<Result> result = new CompletableFuture<>();
            result.completeExceptionally(new NoReferException(request.getHeader(),
                    String.format("alias is not find. className=%s alias=%s", request.getPayLoad().getClassName(), alias),
                    ExceptionCode.CONSUMER_GROUP_NO_REFER));
            return result;
        }
    }

    /**
     * 计算路由信息
     *
     * @param request 请求
     * @return 分组
     */
    protected String router(final RequestMessage<Invocation> request) {
        RequestContext context = request.getContext();
        Invocation invocation = request.getPayLoad();
        boolean generic = invocation.isGeneric();
        //过滤器已经设置好了真实的方法名
        String methodName = invocation.getMethodName();
        MethodGroup group = options.get(methodName);
        // 先从上下文里面取 目标路由参数 dstParam
        String dstParam = context.getAttachment(Constants.HIDDEN_KEY_DST_PARAM);
        if (dstParam == null) {
            // 再从session里取
            dstParam = (String) context.getSession(Constants.HIDDEN_KEY_DST_PARAM);
            if (dstParam == null) {
                // 再从参数里面取
                Integer index = group == null ? null : group.getParameter();
                if (index != null) {
                    // 获取参数值
                    Object[] args = !generic ? invocation.getArgs() : (Object[]) invocation.getArgs()[2];
                    if (index >= 0 && args.length > index) {
                        dstParam = args[index] == null ? null : args[index].toString();
                    } else {
                        throw new RpcException(String.format("Length of args must greater than index of dstParam," +
                                " method : %s.%s", clazz.getName(), invocation.getMethodName()), ExceptionCode.CONSUMER_GROUP_ARGS_INDEX);
                    }
                }
            }
        }

        AliasMeta meta = aliasMeta;
        if (dstParam == null) {
            // 没有目标参数配置，随机全部分组
            return meta.random();
        } else {
            // 有目标参数配置
            String alias = group == null ? null : group.getGroup(dstParam);
            // 映射里找不到 认为传入值就当是参数值
            // 不需要检查分组是否存在，在ConsumerGroupConfig里面会根据自适应参数来动态创建不存在的分组
            return alias == null || alias.isEmpty() ? dstParam : alias;
        }
    }

    /**
     * 方法分组信息
     */
    protected static class MethodGroup {
        /**
         * 分组参数索引
         */
        protected Integer parameter;
        /**
         * 默认分组
         */
        protected volatile String defGroup;
        /**
         * 分组
         */
        protected volatile Map<String, String> groups;

        public MethodGroup(Integer parameter, Map<String, String> groups) {
            this.parameter = parameter;
            this.groups = groups;
            this.defGroup = groups == null ? null : groups.get("*");
        }

        /**
         * 获取分组参数索引
         *
         * @return 参数索引
         */
        public Integer getParameter() {
            return parameter;
        }

        /**
         * 获取分组
         *
         * @param param 参数
         * @return 分组
         */
        public String getGroup(final String param) {
            return groups == null ? defGroup : groups.getOrDefault(param, defGroup);
        }
    }

}
