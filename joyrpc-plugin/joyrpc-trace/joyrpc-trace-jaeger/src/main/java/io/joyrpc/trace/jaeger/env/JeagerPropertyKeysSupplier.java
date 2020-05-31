package io.joyrpc.trace.jaeger.env;

import io.joyrpc.extension.Extension;
import io.joyrpc.spring.context.PropertyKeysSupplier;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

@Extension(value = "jeager-spring")
public class JeagerPropertyKeysSupplier implements PropertyKeysSupplier {


    protected static final String[] JAEGER_CONFIG_KEYS = new String[]{
            "JAEGER_REPORTER_FLUSH_INTERVAL",
            "JAEGER_REPORTER_MAX_QUEUE_SIZE",
            "JAEGER_REPORTER_LOG_SPANS",
            "JAEGER_ENDPOINT",
            "JAEGER_AGENT_HOST",
            "JAEGER_AGENT_PORT",
            "JAEGER_USER",
            "JAEGER_PASSWORD",
            "JAEGER_AUTH_TOKEN",
            "JAEGER_SAMPLER_TYPE",
            "JAEGER_SAMPLER_MANAGER_HOST_PORT",
            "JAEGER_SAMPLER_PARAM"
    };

    @Override
    public String[] propertyKeys() {
        return JAEGER_CONFIG_KEYS;
    }
}
