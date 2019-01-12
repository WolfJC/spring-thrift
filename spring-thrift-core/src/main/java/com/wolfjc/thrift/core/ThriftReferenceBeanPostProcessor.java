package com.wolfjc.thrift.core;

import com.wolfjc.thrift.core.annotation.ThriftReference;
import com.wolfjc.thrift.core.proxy.ProxyFactory;
import com.wolfjc.thrift.core.proxy.jdk.JdkProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 参考Spring中的{@code CommonAnnotationBeanPostProcessor}、incubator-dubbo的{@code AnnotationInjectedBeanPostProcessor}
 */
public class ThriftReferenceBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements ApplicationContextAware,
        MergedBeanDefinitionPostProcessor, BeanFactoryAware, DisposableBean, BeanClassLoaderAware, PriorityOrdered {

    public final static String className = "thriftReferenceBeanPostProcessor";

    private ApplicationContext applicationContext;

    /**
     * 缓存大小，可以通过jvm参数设置
     */
    private final static int SIZE = Integer.getInteger("reference.bean.cache.size", 128);

    /**
     * InjectionMetadata对象缓存
     */
    private final ConcurrentHashMap<String, InjectionMetadata> injectionMetadataCache = new ConcurrentHashMap<>(SIZE);

//    /**
//     * ThriftInjectElement 缓存对象
//     */
//    private final ConcurrentHashMap<String,ThriftInjectElement> thriftInjectElementCache = new ConcurrentHashMap<>(SIZE);


    private final ConcurrentHashMap<String, Object> thriftInjectObjectsCache = new ConcurrentHashMap<>(SIZE);


    private BeanFactory beanFactory;


    private ClassLoader classLoader;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


    @Override
    public void destroy() throws Exception {

        for (Object object : thriftInjectObjectsCache.values()) {
            if (object instanceof DisposableBean) {
                ((DisposableBean) object).destroy();
            }
        }

        injectionMetadataCache.clear();

        thriftInjectObjectsCache.clear();
    }

    /**
     * 核心方法:用来实现自定义注解的注入功能
     *
     * @return
     * @throws BeansException
     */

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

        InjectionMetadata metadata = findInjectMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable throwable) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed");
        }
        return pvs;
    }


    private InjectionMetadata findInjectMetadata(String beanName, final Class<?> clazz, @Nullable PropertyValues pvs) {

        String cacheKey = StringUtils.hasLength(beanName) ? beanName : clazz.getName();

        InjectionMetadata injectionMetadata = this.injectionMetadataCache.get(cacheKey);

        if (InjectionMetadata.needsRefresh(injectionMetadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                injectionMetadata = this.injectionMetadataCache.get(cacheKey);
                //双重检查
                if (InjectionMetadata.needsRefresh(injectionMetadata, clazz)) {

                    if (injectionMetadata != null) {
                        injectionMetadata.clear(pvs);
                    }
                    //重新构建InjectionMetadata对象
                    injectionMetadata = buildInjectionMetadata(clazz);
                    //加入本地缓存中
                    this.injectionMetadataCache.put(cacheKey, injectionMetadata);
                }
            }
        }

        return injectionMetadata;
    }

    /**
     * 构建InjectionMetadata对象
     * <p>
     * dubbo自定义了类{@code AnnotatedInjectionMetadata}和{@code AnnotatedMethodElement},里面包含了注解信息，通过注解信息可以生成dubbo自定义的
     * beanName。
     *
     * @param clazz
     * @return
     */
    private InjectionMetadata buildInjectionMetadata(final Class<?> clazz) {

        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();

        //通过反射遍历对象中的成员变量
        ReflectionUtils.doWithFields(clazz, field -> {
            if (field.isAnnotationPresent(ThriftReference.class)) {
                if (Modifier.isStatic(field.getModifiers())) {
                    throw new IllegalStateException("@ThriftReference annotation is not supported on static fields");
                }
                ThriftInjectElement injectElement = new ThriftInjectElement(field, field.getAnnotation(ThriftReference.class));
                elements.add(injectElement);
            }
        });
        return new InjectionMetadata(clazz, elements);
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        InjectionMetadata metadata = findInjectMetadata(beanName, beanType, null);
        metadata.checkConfigMembers(beanDefinition);
    }


    private Object getInjectObject(ThriftReference annotation, Class injectType) {

        String cacheKey = buildInjectObjectCacheKey(annotation, injectType);

        Object injectObject = thriftInjectObjectsCache.get(cacheKey);

        if (injectObject == null) {
            injectObject = doGetInjectObject(injectType, annotation);
            thriftInjectObjectsCache.putIfAbsent(cacheKey, injectObject);
        }

        return injectObject;
    }


    /**
     * cache key 的生成规则
     *
     * @param annotation
     * @return
     */
    private String buildInjectObjectCacheKey(ThriftReference annotation, Class injectType) {
        return StringUtils.hasLength(annotation.name()) ? annotation.name() : injectType.getName();
    }


    private Object doGetInjectObject(Class injectType, ThriftReference annotation) {
        //先检查Spring容器中是否已经注入了bean，若没有才远程调用
        String referenceBeanName = annotation.name();
        Object bean = beanFactory.getBean(referenceBeanName);
        if (bean != null) {
            return bean;
        }
        ProxyFactory proxyFactory = new JdkProxyFactory();
        Object proxy = proxyFactory.createProxy(injectType);
        return proxy;
    }


    public class ThriftInjectElement extends InjectionMetadata.InjectedElement {

        private Field field;

        private ThriftReference annotation;


//        private volatile Object bean;

        protected ThriftInjectElement(Field field, ThriftReference annotation) {
            super(field, null);
            this.field = field;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object target, String requestingBeanName, PropertyValues pvs) throws Throwable {

            Class<?> injectFiledType = field.getType();

            //todo::自定义注入bean的实例，后期RPC代理实例注入从这里开始
            Object injectObject = getInjectObject(annotation, injectFiledType);

//            String injectBeanName = field.getName();
            //使用BeanFactory获取bean实例
//            Object injectObject = beanFactory.getBean(injectBeanName);

            ReflectionUtils.makeAccessible(field);

            field.set(target, injectObject);
        }
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public int getOrder() {
        //最低优先级,确保ThriftServiceBeanPostProcessor优先执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
