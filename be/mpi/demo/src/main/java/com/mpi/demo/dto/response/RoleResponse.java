package com.mpi.demo.dto.response;

import java.time.LocalDateTime;

import com.mpi.demo.entity.Role;

public record RoleResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }
}
