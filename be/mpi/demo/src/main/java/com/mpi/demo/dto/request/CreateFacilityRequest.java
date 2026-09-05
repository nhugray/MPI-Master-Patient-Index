package com.mpi.demo.dto.request;

import com.mpi.demo.constant.FacilityTypeEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class CreateFacilityRequest {
    @NotBlank(message = "Mã cơ sở không được để trống")
    @Size(max = 50, message = "Mã cơ sở tối đa 50 ký tự")
    private String code;

    @NotBlank(message = "Tên cơ sở không được để trống")
    @Size(max = 255, message = "Tên cơ sở tối đa 255 ký tự")
    private String name;

    private FacilityTypeEnum facilityType;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0")
    private String phoneNumber;

    private Boolean isActive;
}
