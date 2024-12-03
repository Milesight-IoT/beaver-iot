/*
 * Copyright (c) 2011-2021, baomidou (jobob@qq.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.milesight.beaveriot.base.utils.lambada;


import com.milesight.beaveriot.base.annotations.SFunction;
import com.milesight.beaveriot.base.exception.ConfigurationException;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Lambda Analysis tool class
 * <p>
 * This class is copied from the mybatis-plus project (https://github.com/baomidou/mybatis-plus)
 * License: Apache License 2.0
 */
public final class LambdaUtils {

    private LambdaUtils() {
    }

    /**
     * This cache may be cleared at any time
     *
     * @param func The lambda object that needs to be parsed
     * @param <T>  type, the target type of the called Function object
     * @return returns the parsed result
     */
    public static <T> LambdaMeta extract(SFunction<T, ?> func) {
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            SerializedLambda invoke = (SerializedLambda) ReflectionKit.setAccessible(method).invoke(func);
            return new SerializedLambdaMeta(invoke);
        } catch (NoSuchMethodException e) {
            if (func instanceof Proxy funcProxy) return new ProxyLambdaMeta(funcProxy);
            String message = "Cannot find method writeReplace, please make sure that the lambda composite class is currently passed in";
            throw new ConfigurationException(message);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new ConfigurationException("", e);
        }
    }

}
