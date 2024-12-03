package com.milesight.beaveriot.base.page;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * @author leon
 */
public class Sorts {

    @JsonProperty("sorts")
    @JsonDeserialize(contentAs = Order.class)
    private List<Order> orders = new ArrayList<>();

    public Sorts(List<Order> orders) {
        this.orders = orders;
    }

    public Sorts() {
        this.orders = new ArrayList<>();
    }

    public List<Order> getOrders() {
        return orders;
    }

    public Sorts asc(String property) {
        return asc(true, property);
    }

    public Sorts desc(String property) {
        return desc(true, property);
    }

    public Sorts asc(boolean condition, String property) {
        if (condition) {
            orders.add(new Order(Sort.Direction.ASC, property));
        }
        return this;
    }

    public Sorts desc(boolean condition, String property) {
        if (condition) {
            orders.add(new Order(Sort.Direction.DESC, property));
        }
        return this;
    }

    public Sort toSort() {
        if (CollectionUtils.isEmpty(orders)) {
            return Sort.unsorted();
        }
        List<Sort.Order> collect = orders.stream().map(order -> new Sort.Order(order.direction, order.property)).toList();
        return Sort.by(collect);
    }

    @Getter
    public static class Order implements Serializable {
        protected Sort.Direction direction;
        @JsonDeserialize(using = LowerCamelCaseDeserializer.class)
        protected String property;

        public Order() {
        }

        public Order(Sort.Direction direction, String property) {
            this.direction = direction;
            this.property = property;
        }

        public String toSql() {
            return property + " " + direction.name();
        }
    }

    public static class LowerCamelCaseDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            return StringUtils.toCamelCase(value);
        }
    }

}
