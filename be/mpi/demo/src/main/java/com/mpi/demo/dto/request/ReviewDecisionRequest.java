package com.mpi.demo.dto.request;

import com.mpi.demo.constant.MatchDecisionEnum;

import jakarta.validation.constraints.NotNull;

public record ReviewDecisionRequest(
        @NotNull(message = "Decision không được để trống")
        MatchDecisionEnum decision,
        
        String reviewNote,
        
        @NotNull(message = "ReviewerId không được để trống")
        Long reviewerId) {
}
