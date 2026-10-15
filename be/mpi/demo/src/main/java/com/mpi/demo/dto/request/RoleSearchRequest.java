package com.mpi.demo.dto.request;

public record RoleSearchRequest(
        /** Tìm theo tên hoặc mô tả (LIKE, không phân biệt hoa thường). */
        String keyword

) {

}
