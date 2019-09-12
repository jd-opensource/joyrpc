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
import io.joyrpc.context.RequestContext;
import io.joyrpc.context.router.GroupRouterConfiguration;
import io.joyrpc.exception.NoReferException;
import io.joyrpc.exception.RpcException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URLBiOption;
import io.joyrpc.invoker.AbstractGroupInvoker;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.GenericMethodOption;
import io.joyrpc.util.Shutdown;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.METHOD_KEY;

/**
 * 参数分组路由
 */
@Extension("parameter")
public class ParameterGroupInvoker extends AbstractGroupInvoker {

    /**
     * 方法分组参数
     */
    protected GenericMethodOption<Optional<Integer>> option;
    /**
     * 接口级别默认路由参数位置
     */
    protected Integer dstParam;

    @Override
    public void setup() {
        super.setup();
        option = new GenericMethodOption<>(clazz, className, o -> Optional.ofNullable(
                url.getPositiveInt(new URLBiOption<>(METHOD_KEY.apply(o, Constants.DST_PARAM_OPTION.getName()),
                        Constants.DST_PARAM_OPTION.getName(), () -> null))));
        dstParam = url.getInteger(Constants.DST_PARAM_OPTION.getName());
    }

    @Override
    public CompletableFuture<Result> invoke(final RequestMessage<Invocation> request) {
        //选择分组
        String alias = router(request);
        //找到分组配置
        ConsumerConfig config = configMap.get(alias);
        if (config != null) {
            //调用
            return config.getRefer().invoke(request);
        } else if (Shutdown.isShutdown()) {
            return CompletableFuture.completedFuture(new Result(request.getContext(), new ShutdownExecption("Refer is shutdown.", false)));
        } else if (aliasAdaptive) {
            //TODO 为何需要自适应分组，动态创建可能没有相应的服务提供者
            //TODO 并发请求由问题，当一个分组正在创建的时候，另外一个请求来了
            //自适应分组，没有则自动创建分组
            CompletableFuture<Result> resultFuture = new CompletableFuture<>();
            ConsumerConfig newConfig = configMap.computeIfAbsent(alias, s -> {
                ConsumerConfig cfg = consumerFunction.apply(s);
                aliasMeta = aliasMeta.addAndCopy(s);
                return cfg;
            });
            //重新设置分组别名
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.whenComplete((v, t) -> {
                if (t == null) {
                    CompletableFuture<Result> invoke = newConfig.getRefer().invoke(request);
                    invoke.whenComplete((o, s) -> {
                        if (s == null) {
                            resultFuture.complete(o);
                        } else {
                            resultFuture.completeExceptionally(s);
                        }
                    });
                } else {
                    //恢复上下文
                    Result result = new Result(request.getContext(), t);
                    RequestContext.restore(result.getContext());
                    resultFuture.complete(result);
                }
            });
            newConfig.refer(future);
            return resultFuture;
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
     * @param request
     * @return
     */
    protected String router(final RequestMessage<Invocation> request) {
        RequestContext context = request.getContext();
        Invocation invocation = request.getPayLoad();
        boolean generic = invocation.isGeneric();
        //过滤器已经设置好了真实的方法名
        String methodName = invocation.getMethodName();
        // 先从上下文里面取 目标路由参数 dstParam
        String dstParam = context.getAttachment(Constants.HIDDEN_KEY_DST_PARAM);
        if (dstParam == null) {
            // 再从session里取
            dstParam = (String) context.getSession(Constants.HIDDEN_KEY_DST_PARAM);
            if (dstParam == null) {
                // 再从参数里面取
                Optional<Integer> optional = option.get(methodName);
                Integer index = optional == null ? null : optional.orElse(null);
                //方法参数中不存在，从接口参数中取
                if (index == null) {
                    index = this.dstParam;
                }
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

        //TODO 判断泛型调用
        AliasMeta meta = aliasMeta;
        if (dstParam == null) {
            // 没有目标参数配置，随机全部分组
            return meta.random();
        } else {
            // 有目标参数配置
            String alias = GroupRouterConfiguration.GROUP_ROUTER_CONF.get(className, methodName, dstParam);
            // 映射里找不到 认为传入值就当是参数值
            // 不需要检查分组是否存在，在ConsumerGroupConfig里面会根据自适应参数来动态创建不存在的分组
            return alias == null || alias.isEmpty() ? dstParam : alias;
        }
    }

}
