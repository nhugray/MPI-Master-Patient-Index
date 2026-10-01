package com.mpi.demo.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWeightConfigRequest(
        @NotNull(message = "Trọng số Tên không được để trống")
        @DecimalMin(value = "0.00", message = "Trọng số Tên phải >= 0")
        @DecimalMax(value = "100.00", message = "Trọng số Tên phải <= 100")
        BigDecimal nameWeight,

        @NotNull(message = "Trọng số Ngày sinh không được để trống")
        @DecimalMin(value = "0.00", message = "Trọng số Ngày sinh phải >= 0")
        @DecimalMax(value = "100.00", message = "Trọng số Ngày sinh phải <= 100")
        BigDecimal dobWeight,

        @NotNull(message = "Trọng số CCCD không được để trống")
        @DecimalMin(value = "0.00", message = "Trọng số CCCD phải >= 0")
        @DecimalMax(value = "100.00", message = "Trọng số CCCD phải <= 100")
        BigDecimal nationalIdWeight,

        @NotNull(message = "Trọng số Điện thoại không được để trống")
        @DecimalMin(value = "0.00", message = "Trọng số Điện thoại phải >= 0")
        @DecimalMax(value = "100.00", message = "Trọng số Điện thoại phải <= 100")
        BigDecimal phoneWeight,

        @NotNull(message = "Trọng số Địa chỉ không được để trống")
        @DecimalMin(value = "0.00", message = "Trọng số Địa chỉ phải >= 0")
        @DecimalMax(value = "100.00", message = "Trọng số Địa chỉ phải <= 100")
        BigDecimal addressWeight,

        @NotNull(message = "Ngưỡng tự động duyệt không được để trống")
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        BigDecimal autoApprovalThreshold,

        @NotNull(message = "Ngưỡng cần xem xét không được để trống")
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100.00")
        BigDecimal manualReviewThreshold,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @Size(max = 255)
        String createdBy) {
}
