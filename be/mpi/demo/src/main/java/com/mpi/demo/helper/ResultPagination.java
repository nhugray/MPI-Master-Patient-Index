package com.mpi.demo.helper;

import java.util.List;

import org.springframework.data.domain.Page;

public record ResultPagination(Meta meta,
        List<?> result) {

    public record Meta(
            int page,
            int pageSize,
            int pages,
            long total) {
    }

    public static ResultPagination fromPage(Page<?> page) {
        Meta meta = new Meta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());
        return new ResultPagination(meta, page.getContent());
    }
}
