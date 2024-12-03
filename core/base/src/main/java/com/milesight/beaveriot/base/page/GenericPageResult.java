package com.milesight.beaveriot.base.page;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.List;

/**
 * @author leon
 */
@Getter
public class GenericPageResult<T> implements PageResultSpec<T> {

    private Integer pageSize;

    private Integer pageNumber;

    private Long total;

    private List<T> content;

    public GenericPageResult(Integer pageSize, Integer pageNumber, Long total, List<T> list) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.total = total;
        this.content = list;
    }

    public GenericPageResult() {
    }

    public GenericPageResult<T> pageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public GenericPageResult<T> pageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public GenericPageResult<T> total(Long total) {
        this.total = total;
        return this;
    }

    public GenericPageResult<T> list(List<T> list) {
        this.content = list;
        return this;
    }

    @Override
    public List<T> getContent() {
        return content;
    }

    @JsonIgnore
    @Override
    public int getTotalPages() {
        return PageResultSpec.super.getTotalPages();
    }

    public static PageResultSpec<Object> empty() {
        return new GenericPageResult(10, 1, 0L, Collections.emptyList());
    }

    public static <T> PageResultSpec<T> of(Integer pageSize, Integer pageNumber, Long total, List<T> contents) {
        GenericPageResult<T> pageResultSpec = new GenericPageResult<>();
        pageResultSpec.pageSize(pageSize)
                .pageNumber(pageNumber)
                .total(total)
                .list(contents);
        return pageResultSpec;
    }

    public static <T> PageResultSpec<T> of(PageImpl page) {
        return of(page.getSize(), page.getNumber() + 1, page.getTotalElements(), page.getContent());
    }
}
