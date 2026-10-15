package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;

public record UserSearchRequest(
        /** Tìm theo tên hoặc email (LIKE, không phân biệt hoa thường). */
        String keyword,

        GenderEnum gender,

        Long roleId

) {

}
