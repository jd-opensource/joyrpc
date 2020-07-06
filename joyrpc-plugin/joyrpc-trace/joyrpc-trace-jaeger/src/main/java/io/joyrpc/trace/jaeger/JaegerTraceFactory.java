package io.joyrpc.trace.jaeger;

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

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.context.Variable;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.TraceFactory;
import io.joyrpc.trace.Tracer;
import io.joyrpc.util.StringUtils;
import io.joyrpc.util.SystemClock;

import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.constants.Constants.KEY_APPNAME;

/**
 * jaeger跟踪工厂
 */
@Extension(value = "jaeger", order = TraceFactory.ORDER_JAEGER)
@ConditionalOnClass("io.jaegertracing.internal.JaegerTracer")
public class JaegerTraceFactory implements TraceFactory {
    /**
     * 隐藏属性的key：分布式跟踪 数据KEY
     */
    public static final String HIDDEN_KEY_TRACE_JAEGER = ".traceJaeger";
    public static final String TRACE_JAEGER = "jaeger";
    public static final String TRACE_ID_HIGH = "traceIdHigh";
    public static final String TRACE_ID_LOW = "traceIdLow";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_ID = "parentId";
    public static final String FLAGS = "flags";
    public static final String ERROR = "error";
    protected JaegerTracer tracer;

    public JaegerTraceFactory() {
        Map<String, Object> global = GlobalContext.getContext();
        Object obj = global.get(TRACE_JAEGER);
        if (obj instanceof JaegerTracer) {
            //判断是否有全局的变量
            tracer = (JaegerTracer) obj;
        } else {
            Parametric parametric = Variable.VARIABLE;
            Configuration configuration = new Configuration(parametric.getString("appService", KEY_APPNAME, "unknown"));
            configuration.withSampler(buildSamplerConfiguration(parametric));
            configuration.withReporter(buildReporterConfiguration(parametric));
            tracer = configuration.getTracer();
        }
    }

    /**
     * 构造报告配置
     *
     * @param parametric 参数
     * @return 报告配置
     */
    protected ReporterConfiguration buildReporterConfiguration(final Parametric parametric) {
        return new ReporterConfiguration()
                .withSender(buildSenderConfiguration(parametric))
                .withFlushInterval(parametric.getPositive("JAEGER_REPORTER_FLUSH_INTERVAL", RemoteReporter.DEFAULT_FLUSH_INTERVAL_MS))
                .withMaxQueueSize(parametric.getPositive("JAEGER_REPORTER_MAX_QUEUE_SIZE", RemoteReporter.DEFAULT_MAX_QUEUE_SIZE))
                .withLogSpans(parametric.getBoolean("JAEGER_REPORTER_LOG_SPANS", true));
    }

    /**
     * 构造发送配置
     *
     * @param parametric 参数
     * @return 发送配置
     */
    protected Configuration.SenderConfiguration buildSenderConfiguration(final Parametric parametric) {
        String endpoint = parametric.getString("JAEGER_ENDPOINT");
        if (!StringUtils.isEmpty(endpoint)) {
            try {
                URL url = URL.valueOf(endpoint, "http");
                if (StringUtils.isEmpty(url.getPath())) {
                    url = url.setPath("/api/traces");
                }
                endpoint = url.toString();
            } catch (Throwable e) {
            }
        }
        return new Configuration.SenderConfiguration()
                .withAgentHost(parametric.getString("JAEGER_AGENT_HOST"))
                .withAgentPort(parametric.getInteger("JAEGER_AGENT_PORT"))
                .withEndpoint(endpoint)
                .withAuthUsername(parametric.getString("JAEGER_USER"))
                .withAuthPassword(parametric.getString("JAEGER_PASSWORD"))
                .withAuthToken(parametric.getString("JAEGER_AUTH_TOKEN"));
    }

    /**
     * 构造采样配置
     *
     * @param parametric 参数
     * @return 采样配置
     */
    protected SamplerConfiguration buildSamplerConfiguration(final Parametric parametric) {
        return new SamplerConfiguration()
                .withType(parametric.getString("JAEGER_SAMPLER_TYPE", "probabilistic"))
                .withManagerHostPort(parametric.getString("JAEGER_SAMPLER_MANAGER_HOST_PORT"))
                .withParam(parametric.getDouble("JAEGER_SAMPLER_PARAM", 0.001D));
    }

    @Override
    public Tracer create(final RequestMessage<Invocation> request) {
        return request.isConsumer() ? new ConsumerTracer(tracer, request) :
                new ProviderTracer(tracer, request);
    }

    /**
     * 抽象的跟踪
     */
    protected static abstract class AbstractTracer implements Tracer {
        protected JaegerTracer tracer;
        protected RequestMessage<Invocation> request;
        protected Invocation invocation;
        protected JaegerSpan span;

        public AbstractTracer(final JaegerTracer tracer,
                              final RequestMessage<Invocation> request) {
            this.tracer = tracer;
            this.request = request;
            this.invocation = request.getPayLoad();
        }

        @Override
        public void begin(final String name, final String component, final Map<String, String> tags) {
            //拿到当前请求的跟踪span
            JaegerSpanContext parent = reject();
            //构建新span
            span = tracer.buildSpan(name).withStartTimestamp(SystemClock.microTime()).asChildOf(parent).start();
            //注入新的span
            inject(span.context());
            //标签
            tag(tags);
        }

        @Override
        public void restore() {
            tracer.activateSpan(span);
        }

        /**
         * 打标签
         *
         * @param tags 标签
         */
        protected void tag(final Map<String, String> tags) {
            if (tags != null) {
                tags.forEach((key, value) -> span.setTag(key, value));
            }
        }

        @Override
        public void end(final Throwable throwable) {
            if (throwable != null) {
                span.setTag(ERROR, true);
                span.log(throwable.getMessage());
            }
            span.finish();
        }

        /**
         * 注入分布式跟踪
         *
         * @param jsc 跟踪上下文
         */
        protected void inject(final JaegerSpanContext jsc) {

        }

        /**
         * 构建调用传递的上下文，返回Object，方便继承
         *
         * @param jsc 上下文
         * @return 传递的上下文
         */
        protected Object build(JaegerSpanContext jsc) {
            Map<String, Object> ctx = new HashMap<>(5);
            ctx.put(TRACE_ID_HIGH, jsc.getTraceIdHigh());
            ctx.put(TRACE_ID_LOW, jsc.getTraceIdLow());
            ctx.put(SPAN_ID, jsc.getSpanId());
            ctx.put(PARENT_ID, jsc.getParentId());
            ctx.put(FLAGS, jsc.getFlags());
            return ctx;
        }

        /**
         * 解析跟踪上下文
         *
         * @return 跟踪上下文
         */
        protected JaegerSpanContext reject() {
            Map<String, Object> ctx = (Map<String, Object>) invocation.removeAttachment(HIDDEN_KEY_TRACE_JAEGER);
            return ctx == null ? null : new JaegerSpanContext(
                    (Long) ctx.get(TRACE_ID_HIGH),
                    (Long) ctx.get(TRACE_ID_LOW),
                    (Long) ctx.get(SPAN_ID),
                    (Long) ctx.get(PARENT_ID),
                    (Byte) ctx.get(FLAGS));
        }
    }

    /**
     * 消费者跟踪
     */
    protected static class ConsumerTracer extends AbstractTracer {

        public ConsumerTracer(final JaegerTracer tracer,
                              final RequestMessage<Invocation> request) {
            super(tracer, request);
        }

        @Override
        protected void inject(final JaegerSpanContext jsc) {
            invocation.addAttachment(HIDDEN_KEY_TRACE_JAEGER, build(jsc));
        }
    }

    /**
     * 生产者跟踪
     */
    protected static class ProviderTracer extends AbstractTracer {

        public ProviderTracer(final JaegerTracer tracer,
                              final RequestMessage<Invocation> request) {
            super(tracer, request);
        }

        @Override
        protected void inject(final JaegerSpanContext jsc) {
            //保存到请求上下文，可以继续传递
            request.getContext().setAttachment(HIDDEN_KEY_TRACE_JAEGER, build(jsc));
        }
    }
}
