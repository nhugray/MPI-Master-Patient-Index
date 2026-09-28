package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.With;

import java.time.LocalDate;

@With
public record UpdatePatientMasterRequest(
                @NotNull(message = "ID không được để trống") Long id,

                @NotBlank(message = "Họ và tên là bắt buộc") @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự") String fullName,

                @NotNull(message = "Ngày sinh là bắt buộc") LocalDate dateOfBirth,

                GenderEnum gender,

                @Pattern(regexp = "^\\d{12}$", message = "CCCD phải gồm 12 chữ số") String nationalId,

                @Size(max = 20, message = "Số BHYT không được vượt quá 20 ký tự") String healthInsuranceNo,

                @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0") String phoneNumber,

                @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự") String address) {

}
