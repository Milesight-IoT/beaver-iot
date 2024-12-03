package com.milesight.beaveriot.base.page;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * @author leon
 */
@Data
public class GenericPageRequest implements PageSpec {

    protected Long offset;

    protected Integer limit;

    protected Integer pageSize = 10;

    protected Integer pageNumber = 1;
    @JsonUnwrapped
    protected Sorts sort;
    /**
     * Total number. If the current end submits total, the back end will not perform count calculation.
     */
    protected Long total;

    @Override
    public boolean isCount() {
        return total == null || total < 0;
    }

    @Override
    public Long getOffset() {
        if (this.offset == null && this.pageNumber != null && this.pageSize != null) {
            this.offset = (long) this.pageSize * (this.pageNumber - 1);
        }
        return this.offset;
    }

    @Override
    public Integer getLimit() {
        if (this.limit == null && this.pageSize != null) {
            this.limit = this.pageSize;
        }
        return this.limit;
    }

    @Override
    public boolean isPaged() {
        return this.pageNumber != null && this.pageSize != null && this.pageNumber >= 0 && this.pageSize > 0;
    }

    public GenericPageRequest total(Long total) {
        this.total = total;
        return this;
    }

    public GenericPageRequest offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public GenericPageRequest limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public GenericPageRequest pageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public GenericPageRequest pageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public GenericPageRequest sort(Sorts sort) {
        this.sort = sort;
        return this;
    }

    public Pageable toPageable() {
        return PageRequest.of(this.pageNumber - 1, this.pageSize, this.sort.toSort());
    }

}
