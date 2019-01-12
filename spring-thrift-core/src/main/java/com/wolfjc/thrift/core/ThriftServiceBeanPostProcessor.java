package com.wolfjc.thrift.core;

import com.wolfjc.thrift.core.annotation.ThriftService;
import com.wolfjc.thrift.core.rpc.ThriftServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;


/**
 * 根据{@code ThriftService}
 */
public class ThriftServiceBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        BeanClassLoaderAware, ResourceLoaderAware{

    public final static String className = "thriftServiceBeanPostProcessor";

    private ClassLoader classLoader;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private Set<String> packagesToScan;

    private ThriftServer thriftServer = ThriftServer.getInstance();

    public ThriftServiceBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }


    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {

        Set<String> resolvePackagesToScan = resolvePackagesToScan(this.packagesToScan);

        if (CollectionUtils.isEmpty(resolvePackagesToScan)) {
            return;
        }

        registerServiceBean(beanDefinitionRegistry, resolvePackagesToScan);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {


    }

    private void registerServiceBean(BeanDefinitionRegistry beanDefinitionRegistry, Set<String> packagesToScan) {

        ThriftClassPathBeanDefinitionScanner scanner = new ThriftClassPathBeanDefinitionScanner(beanDefinitionRegistry,
                environment, resourceLoader);

        scanner.addIncludeFilter(new AnnotationTypeFilter(ThriftService.class));

        //TODO::这里先默认使用AnnotationBeanNameGenerator，后面了解不同的BeanNameGenerator作用之后再做调整
        BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

        scanner.setBeanNameGenerator(beanNameGenerator);

        packagesToScan.forEach(packageToScan -> {
            Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);

            Set<BeanDefinitionHolder> beanDefinitionHolders = beanDefinitions.stream().map(beanDefinition -> {
                String beanName = beanNameGenerator.generateBeanName(beanDefinition, beanDefinitionRegistry);
                return new BeanDefinitionHolder(beanDefinition, beanName);
            }).collect(Collectors.toSet());

            if (CollectionUtils.isEmpty(beanDefinitionHolders)) {

            } else {
                beanDefinitionHolders.forEach(beanDefinitionHolder -> registerBean(beanDefinitionHolder,
                        scanner, beanDefinitionRegistry));
                //远程暴露所有的Thrift服务
                thriftServer.export();
            }
        });

    }

    private void registerBean(BeanDefinitionHolder beanDefinitionHolder, ThriftClassPathBeanDefinitionScanner scanner,
                              BeanDefinitionRegistry registry) {
        Class<?> beanClass = resolveClassName(beanDefinitionHolder);

        ThriftService thriftService = AnnotationUtils.findAnnotation(beanClass, ThriftService.class);

        Class<?> interfaceClass = resolveServiceInterfaceClass(beanClass, thriftService);

        //todo::这里可以自定义beanDefinition以及beanName
//        String serviceBeanName = beanDefinitionHolder.getBeanName();
//        BeanDefinition beanDefinition = buildServiceBeanDefinition(serviceBeanName, interfaceClass);
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        //使用注解中的别名
        String registerBeanName = thriftService.name();
        //检查是否已经注册了bean
        if (scanner.checkCandidate(registerBeanName, beanDefinition)) {
            registry.registerBeanDefinition(registerBeanName, beanDefinition);
            thriftServer.registerService(new ServiceMeta(beanClass,interfaceClass));
        }
    }


    private Class<?> resolveServiceInterfaceClass(Class<?> beanClass, ThriftService thriftService) {
        //todo::可以在注解中申明interfaceClass或者interface class name
        //dubbo在注解中定以了interfaceClass 和 interfaceName,若都未定义则取所有实现接口的一个。
        Class<?> interfaceClass = null;

        String name = thriftService.name();
        if (StringUtils.hasText(name)) {
            if (ClassUtils.isPresent(name, classLoader)) {
                interfaceClass = ClassUtils.resolveClassName(name,classLoader);
                return interfaceClass;
            }
        }
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length > 0){
            interfaceClass = interfaces[0];
        }
        return interfaceClass;
    }


    private BeanDefinition buildServiceBeanDefinition(String serviceBeanName, Class<?> beanClass) {
        BeanDefinitionBuilder beanDefinitionBuilder = rootBeanDefinition(ServiceBean.class);
        String resolvedBeanName = environment.resolvePlaceholders(serviceBeanName);
        beanDefinitionBuilder.addPropertyReference("ref", resolvedBeanName);
//        beanDefinitionBuilder.addPropertyValue("interface", beanClass.getName());
        return beanDefinitionBuilder.getBeanDefinition();
    }


    /**
     * @param beanDefinitionHolder
     * @return
     */
    private Class<?> resolveClassName(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return ClassUtils.resolveClassName(beanClassName, classLoader);
    }


    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        return packagesToScan.stream()
                .filter(packageToScan -> StringUtils.hasText(packageToScan))
                .map(packageToScan -> environment.resolvePlaceholders(packageToScan.trim()))
                .collect(Collectors.toSet());
    }


    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
