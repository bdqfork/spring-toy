package cn.bdqfork.core.aop.proxy;

import cn.bdqfork.core.container.UnSharedInstance;
import cn.bdqfork.core.exception.BeansException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Cglib代理
 *
 * @author bdq
 * @since 2019-02-14
 */
public class CglibMethodInterceptor extends AbstractProxyInvocationHandler implements MethodInterceptor {
    private AdviceInvocationHandler adviceInvocationHandler;

    public CglibMethodInterceptor(AdviceInvocationHandler adviceInvocationHandler) {
        this.adviceInvocationHandler = adviceInvocationHandler;
    }

    /**
     * 创建代理实例
     *
     * @return Object 代理实例
     */
    @Override
    public Object newProxyInstance() throws BeansException {
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(this);

        Class targetClass = target.getClass();

        Object[] args = null;
        if (targetClass == UnSharedInstance.class) {

            UnSharedInstance unSharedInstance = (UnSharedInstance) target;
            targetClass = unSharedInstance.getClazz();
            args = unSharedInstance.getArgs();

        }

        enhancer.setSuperclass(targetClass);

        if (args == null) {
            return enhancer.create();
        }
        Class[] argumentTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

        return enhancer.create(argumentTypes, args);
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object targetObject = getTargetObject();
        Object result = invokeObjectMethod(targetObject, method, args);
        if (result == null) {
            result = adviceInvocationHandler.invokeWithAdvice(targetObject, method, args);
        }
        return result;
    }

}