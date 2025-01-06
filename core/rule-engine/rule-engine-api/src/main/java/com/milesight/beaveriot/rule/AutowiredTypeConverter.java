package com.milesight.beaveriot.rule;

import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.spi.TypeConvertible;
import org.apache.camel.support.TypeConverterSupport;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;

/**
 * @author leon
 */
public abstract class AutowiredTypeConverter<S, T> extends TypeConverterSupport {

    private final Class<?> fromClass;
    private final Class<?> toClass;

    protected AutowiredTypeConverter() {
        super();
        ResolvableType resolvableType = ResolvableType.forClass(this.getClass());
        this.fromClass = resolvableType.getSuperType().resolveGeneric(0);
        this.toClass = resolvableType.getSuperType().resolveGeneric(1);
        Assert.notNull(fromClass, "fromClass cannot be null");
        Assert.notNull(toClass, "toClass cannot be null");
    }

    public abstract T doConvertTo(Class<T> type, Exchange exchange, S value) throws TypeConversionException;

    public TypeConvertible getTypeConvertible() {
        return new TypeConvertible(fromClass, toClass);
    }

    @Override
    public <A> A convertTo(Class<A> type, Exchange exchange, Object value) throws TypeConversionException {
        return (A) doConvertTo((Class<T>) type, exchange, (S) value);
    }
}
