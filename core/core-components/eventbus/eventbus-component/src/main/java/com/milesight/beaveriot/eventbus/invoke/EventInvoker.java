package com.milesight.beaveriot.eventbus.invoke;

import com.milesight.beaveriot.eventbus.api.Event;
import com.milesight.beaveriot.eventbus.api.IdentityKey;

import java.lang.reflect.InvocationTargetException;

/**
 * @author leon
 */
public interface EventInvoker<T extends Event<? extends IdentityKey>> {

    Object invoke(T event, String[] matchMultiKeys) throws InvocationTargetException, IllegalAccessException ;

}
