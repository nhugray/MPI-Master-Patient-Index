package com.mpi.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSourceSystemRequest {
    @NotNull(message = "Id không được để trống")
    private Long id;

    @NotNull(message = "Facility ID không được để trống")
    private Long facilityId;

    @NotBlank(message = "Mã hệ thống nguồn không được để trống")
    @Size(max = 50, message = "Mã hệ thống nguồn tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên hệ thống nguồn không được để trống")
    @Size(max = 255, message = "Tên hệ thống nguồn tối đa 255 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    private Boolean isActive;
}
