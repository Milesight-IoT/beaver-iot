package com.milesight.beaveriot.base.annotations;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Functions that support serialization
 *
 * @author leon
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {
}
