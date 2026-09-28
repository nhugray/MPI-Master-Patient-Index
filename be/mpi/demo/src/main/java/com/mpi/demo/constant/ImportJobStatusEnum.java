package com.mpi.demo.constant;

public enum ImportJobStatusEnum {
    PENDING,      // Đang chờ xử lý
    VALIDATING,   // Đang validate dữ liệu
    PROCESSING,   // Đang xử lý import
    COMPLETED,    // Hoàn thành
    FAILED,       // Thất bại
    CANCELLED     // Đã hủy
}
