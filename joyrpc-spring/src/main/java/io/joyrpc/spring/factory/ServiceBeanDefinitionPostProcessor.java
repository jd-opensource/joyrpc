package io.joyrpc.spring.factory;

import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionPointLazy;
import io.joyrpc.spring.annotation.Consumer;
import io.joyrpc.spring.annotation.Provider;
import io.joyrpc.spring.context.DefaultClassPathBeanDefinitionScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 注解扫描处理类
 */
public class ServiceBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        ResourceLoaderAware, BeanClassLoaderAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final ExtensionPoint<ServiceBeanDefinitionProcessor, String> REGISTRY_PROCESSOR = new ExtensionPointLazy<>(ServiceBeanDefinitionProcessor.class);

    private Environment environment;

    private ResourceLoader resourceLoader;

    private final Set<String> basePackages;

    private ClassLoader classLoader;


    /**
     * 构造方法
     *
     * @param basePackages
     */
    public ServiceBeanDefinitionPostProcessor(Set<String> basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        //收集packagesToScan配置，获取rpc要扫描的包路径
        Set<String> packages = new LinkedHashSet<>(basePackages.size());
        for (String packageToScan : basePackages) {
            if (org.springframework.util.StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                packages.add(resolvedPackageToScan);
            }
        }
        //处理包下的class类
        if (!CollectionUtils.isEmpty(packages)) {
            processPackages(packages, registry);
        } else {
            logger.warn("packagesToScan is empty , ProviderBean registry will be ignored!");
        }
    }

    /**
     * 处理rpc扫描的包下的class类
     *
     * @param packages
     * @param registry
     */
    private void processPackages(Set<String> packages, BeanDefinitionRegistry registry) {
        //构造
        DefaultClassPathBeanDefinitionScanner scanner = new DefaultClassPathBeanDefinitionScanner(registry, environment, resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
        scanner.addIncludeFilter(new AnnotationFilter(Consumer.class));
        //获取配置的rpc扫描包下的所有bean定义
        for (String basePackage : packages) {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(basePackage);
            if (!CollectionUtils.isEmpty(beanDefinitions)) {
                for (BeanDefinition beanDefinition : beanDefinitions) {
                    REGISTRY_PROCESSOR.extensions().forEach(
                            registryProcessor -> registryProcessor.processBean(beanDefinition, registry, environment, classLoader));
                }
            } else {
                logger.warn(String.format("No Spring Bean annotating @Provider was found under package %s", basePackage));
            }
        }

    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 扫描类过滤（主要用来过滤含有某一个注解的类）
     */
    protected class AnnotationFilter implements TypeFilter {

        protected Class<? extends Annotation> annotationType;

        protected AnnotationFilter(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        @Override
        public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
            ClassMetadata classMetadata = metadataReader.getClassMetadata();
            if (!classMetadata.isInterface()
                    && !classMetadata.isAnnotation()
                    && !classMetadata.isFinal()) {
                Class clazz = resolveClassName(classMetadata.getClassName(), classLoader);
                if (clazz.getAnnotation(annotationType) != null) {
                    return true;
                }
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (!Modifier.isFinal(field.getModifiers())
                            && !Modifier.isStatic(field.getModifiers())
                            && field.getAnnotation(annotationType) != null) {
                        return true;
                    }
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (method.getAnnotation(annotationType) != null) {
                        return true;
                    }
                }

            }
            return false;
        }
    }
}
