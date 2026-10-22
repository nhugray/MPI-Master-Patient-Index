package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.constant.UserStatusEnum;

public record UserSearchRequest(
        String keyword,

        GenderEnum gender,

        UserStatusEnum status,

        Long roleId

) {

}
