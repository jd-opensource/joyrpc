package io.joyrpc.spring.processor;

import io.joyrpc.spring.ConsumerBean;
import io.joyrpc.spring.ConsumerGroupBean;
import io.joyrpc.spring.GlobalParameterBean;
import io.joyrpc.spring.ProviderBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DependsOn处理类
 */
public class InterfaceBeanDependsOn {

    /**
     * DependsOn处理类处理类类别
     */
    protected static final Map<BeanDefinitionRegistry, InterfaceBeanDependsOn> interfaceBeanDependsOns = new ConcurrentHashMap<>();

    /**
     * ConsumerBean定义列表
     */
    private Set<BeanDefinition> consumers = new HashSet<>();
    /**
     * ConsumerGroupBean定义列表
     */
    private Set<BeanDefinition> consumerGroups = new HashSet<>();
    /**
     * ProviderBean定义列表
     */
    private Set<BeanDefinition> providers = new HashSet<>();
    /**
     * GlobalParameterBean bean名称列表
     */
    private Set<String> contextNames = new HashSet<>();
    /**
     * 开关
     */
    private AtomicBoolean onDependsOn = new AtomicBoolean();
    /**
     * registry
     */
    private BeanDefinitionRegistry registry;

    /**
     * 构造方法
     *
     * @param registry
     */
    public InterfaceBeanDependsOn(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    /**
     * dependsOn操作
     */
    public void doDependsOn() {
        if (onDependsOn.compareAndSet(false, true)) {
            String[] names = registry.getBeanDefinitionNames();
            for (String name : names) {
                BeanDefinition definition = registry.getBeanDefinition(name);
                String beanClassName = definition.getBeanClassName();
                if (ConsumerBean.class.getName().equals(beanClassName)) {
                    consumers.add(definition);
                } else if (ProviderBean.class.getName().equals(beanClassName)) {
                    providers.add(definition);

                } else if (ConsumerGroupBean.class.getName().equals(beanClassName)) {
                    consumerGroups.add(definition);
                } else if (GlobalParameterBean.class.getName().equals(beanClassName)) {
                    contextNames.add(name);
                }
            }
            String[] dependOnNames = contextNames.toArray(new String[0]);
            consumers.forEach(definition -> definition.setDependsOn(dependOnNames));
            consumerGroups.forEach(definition -> definition.setDependsOn(dependOnNames));
            providers.forEach(definition -> definition.setDependsOn(dependOnNames));
        }
    }

    /**
     * 获取 InterfaceBeanDependsOn
     *
     * @param registry
     * @return
     */
    public static InterfaceBeanDependsOn getOrCreate(BeanDefinitionRegistry registry) {
        return interfaceBeanDependsOns.computeIfAbsent(registry, InterfaceBeanDependsOn::new);
    }


}
