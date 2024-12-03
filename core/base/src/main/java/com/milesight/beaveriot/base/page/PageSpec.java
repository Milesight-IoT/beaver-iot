package com.milesight.beaveriot.base.page;


/**
 * Paging request interface specification
 *
 * @author leon
 */
public interface PageSpec {
    default boolean isPaged() {
        return true;
    }

    /**
     * Whether to count, by default, count statistics will not be performed when the total value is submitted by the current end (applicable to scenarios with large amounts of data)
     *
     * @return
     */
    default boolean isCount() {
        return true;
    }

    Long getOffset();

    Integer getLimit();

    Integer getPageSize();

    /**
     * The paging page number starts from 1 (note: Spring pageable starts from 0)
     *
     * @return
     */
    Integer getPageNumber();

    Sorts getSort();

    Long getTotal();
}
