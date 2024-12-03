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


import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.AccessibleObject;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 *  This class is copied from the mybatis-plus project (https://github.com/baomidou/mybatis-plus)
 *  License: Apache License 2.0
 * Reflection tool class, providing quick operations related to reflection
 */
@Slf4j
public final class ReflectionKit {

    private ReflectionKit() {
    }

    /**
     * Set the accessibility permission of the accessible object to true
     *
     * @param object accessible object
     * @param <T>    type
     * @return returns the set object
     */
    public static <T extends AccessibleObject> T setAccessible(T object) {
        return AccessController.doPrivileged(new SetAccessibleAction<>(object));
    }

    public static class SetAccessibleAction<T extends AccessibleObject> implements PrivilegedAction<T> {
        private final T obj;

        public SetAccessibleAction(T obj) {
            this.obj = obj;
        }

        @Override
        public T run() {
            obj.setAccessible(true);
            return obj;
        }
    }
}
