package com.mpi.demo.dto.response;

import java.time.LocalDateTime;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.entity.User;

public record RegisterResponse(
        Long id,
        String name,
        String email,
        GenderEnum gender,
        String address,
        LocalDateTime createdAt) {
    public static RegisterResponse fromEntity(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getGender(),
                user.getAddress(),
                user.getCreatedAt());
    }
}
