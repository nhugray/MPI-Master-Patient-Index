package com.mpi.demo.dto.request;

import java.time.LocalDate;

import com.mpi.demo.constant.GenderEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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
public class UpdatePatientRequest {

    @NotNull(message = "Id không được để trống")
    private Long id;

    @NotNull(message = "ID hệ thống nguồn không được để trống")
    private Long sourceSystemId;

    @NotBlank(message = "Mã bệnh nhân cục bộ không được để trống")
    @Size(max = 100, message = "Mã bệnh nhân cục bộ tối đa 100 ký tự")
    private String localPatientCode;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    private GenderEnum gender;

    @Pattern(regexp = "\\d{12}", message = "Số CCCD phải gồm 12 chữ số")
    private String nationalId;

    @Size(max = 20, message = "Mã BHYT tối đa 20 ký tự")
    private String healthInsuranceNo;

    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0")
    private String phoneNumber;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;
}
