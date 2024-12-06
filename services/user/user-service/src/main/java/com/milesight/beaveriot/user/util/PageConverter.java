package com.milesight.beaveriot.user.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/4 17:20
 */
public class PageConverter {

    public static <T> Page<T> convertToPage(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());

        List<T> subList = list.subList(start, end);

        return new PageImpl<>(subList, pageable, list.size());
    }

}
