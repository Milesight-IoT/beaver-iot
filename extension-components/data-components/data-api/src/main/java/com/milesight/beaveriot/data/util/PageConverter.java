package com.milesight.beaveriot.data.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author loong
 * @date 2024/12/4 17:20
 */
@Slf4j
public class PageConverter {

    public static <T> Page<T> convertToPage(List<T> list, Pageable pageable) {
        if (list == null || list.isEmpty()) {
            return Page.empty();
        }
        List<T> sortList = new ArrayList<>(list);
        if (pageable.getSort().isSorted()) {
            sortList = sortList(sortList, pageable.getSort());
        }
        int offset = (int) pageable.getOffset();
        int start = offset;
        if (offset >= sortList.size()) {
            start = 0;
            pageable = PageRequest.of(0, pageable.getPageSize(), pageable.getSort());
        }
        int end = Math.min((start + pageable.getPageSize()), sortList.size());

        List<T> subList = sortList.subList(start, end);

        return new PageImpl<>(subList, pageable, sortList.size());
    }

    private static <T> List<T> sortList(List<T> list, Sort sort) {
        Comparator<T> comparator = (o1, o2) -> {
            int result = 0;
            for (Sort.Order order : sort) {
                result = compare(order.getProperty(), o1, o2, order.isAscending());
                if (result != 0) {
                    return result;
                }
            }
            return result;
        };
        List<T> sortedList = new ArrayList<>(list);
        sortedList.sort(comparator);
        return sortedList;
    }

    private static <T> int compare(String property, T o1, T o2, boolean ascending) {
        try {
            Field field = o1.getClass().getDeclaredField(property);
            field.setAccessible(true);
            Comparable<Object> value1 = (Comparable<Object>) field.get(o1);
            Comparable<Object> value2 = (Comparable<Object>) field.get(o2);

            int result = value1.compareTo(value2);
            return ascending ? result : -result;
        } catch (Exception e) {
            log.error("Failed to compare property: " + property, e);
        }
        return 0;
    }

}
