package com.mpi.demo.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.mpi.demo.constant.GenderEnum;
import com.mpi.demo.entity.User;

public record UserResponse(
        Long id,
        String name,
        String email,
        String address,
        GenderEnum gender,
        String avatar,
        List<RoleInfo> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public record RoleInfo(Long id, String name) {
    }

    public static UserResponse fromEntity(User user) {

        List<RoleInfo> roleInfos = user.getRoles().stream()
                .map(role -> new RoleInfo(role.getId(), role.getName()))
                .toList();

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAddress(),
                user.getGender(),
                user.getAvatar(),
                roleInfos,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
