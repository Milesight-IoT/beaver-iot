package com.milesight.beaveriot.base.utils.lambada;

/**
 * This class is copied from the mybatis-plus project (https://github.com/baomidou/mybatis-plus)
 * License: Apache License 2.0
 */
public interface LambdaMeta {

    /**
     * Get the name of the lambda expression implementation method
     *
     * @return The implementation method name corresponding to the lambda expression
     */
    String getImplMethodName();

    /**
     * The class that instantiates this method
     *
     * @return returns the corresponding class name
     */
    Class<?> getInstantiatedClass();

}
