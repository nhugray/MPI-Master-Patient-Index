package com.mpi.demo.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken) {
}
