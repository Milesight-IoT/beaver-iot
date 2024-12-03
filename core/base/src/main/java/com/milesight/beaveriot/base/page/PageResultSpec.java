package com.milesight.beaveriot.base.page;

import java.util.List;

/**
 * Pagination response interface specification
 *
 * @author leon
 */
public interface PageResultSpec<T> {
    Integer getPageSize();

    Integer getPageNumber(); // start from 1

    Long getTotal();

    List<T> getContent();

    default int getTotalPages() {
        return getPageSize() == 0 ? 0 : (int) Math.ceil((double) getTotal() / (double) getPageSize());
    }

}
