package site.xleon.future.ctp.core;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

@FunctionalInterface
public interface IMyFunction<T,R> extends Function<T,R>, Serializable {
    default SerializedLambda getSerializedLambda() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        return (SerializedLambda) method.invoke(this);
    }

    default String getMethodName() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        SerializedLambda serializedLambda = getSerializedLambda();
        String methodName = serializedLambda.getImplMethodName();
        String className = serializedLambda.getImplClass();
        return methodName;
    }
}