package cc.lovezhy.raft.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class RpcProvider<T> {

    private final Logger log = LoggerFactory.getLogger(RpcProvider.class);

    static <T> RpcProvider<T> create(Class<T> providerClazz) {
        Preconditions.checkNotNull(providerClazz);
        return new RpcProvider<>(providerClazz);
    }

    static <T> RpcProvider<T> create(T providerBean) {
        Preconditions.checkNotNull(providerBean);
        return new RpcProvider<>(providerBean);
    }

    private Object instance;
    private Map<String, Method> methodMap;

    private RpcProvider(T providerBean) {
        this.methodMap = Maps.newHashMap();

        Class<?> providerClazz = providerBean.getClass().getInterfaces()[0];
        for (Method method : providerClazz.getDeclaredMethods()) {
            methodMap.put(method.getName(), method);
        }
        this.instance = providerBean;
    }

    private RpcProvider(Class<T> providerClazz) {
        this.methodMap = Maps.newHashMap();
        Class<?> interfaceClazz = providerClazz.getInterfaces()[0];
        for (Method method : interfaceClazz.getDeclaredMethods()) {
            methodMap.put(method.getName(), method);
        }
        try {
            this.instance = providerClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
    }

    Object invoke(String methodName, Object[] params) {
        Method method = methodMap.get(methodName);
        try {
            return method.invoke(instance, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
        }
        return new IllegalStateException();
    }


}
