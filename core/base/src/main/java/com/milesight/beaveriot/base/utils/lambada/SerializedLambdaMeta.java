package com.milesight.beaveriot.base.utils.lambada;

import com.milesight.beaveriot.base.exception.ConfigurationException;

import java.lang.invoke.SerializedLambda;

/**
 *  This class is copied from the mybatis-plus project (https://github.com/baomidou/mybatis-plus)
 *  License: Apache License 2.0
 * Created by hcl at 2021/5/14
 */
public class SerializedLambdaMeta implements LambdaMeta {

    private final SerializedLambda lambda;

    public SerializedLambdaMeta(SerializedLambda lambda) {
        this.lambda = lambda;
    }

    @Override
    public String getImplMethodName() {
        return lambda.getImplMethodName();
    }

    @Override
    public Class<?> getInstantiatedClass() {
        String instantiatedMethodType = lambda.getInstantiatedMethodType();
        String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(';')).replace('/', '.');
        return toClassConfident(instantiatedType, this.getClass().getClassLoader());
    }

    public static Class<?> toClassConfident(String className, ClassLoader classLoader) {

        try {
            return Class.forName(className, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Cannot find class: " + className);
        }
    }
}
