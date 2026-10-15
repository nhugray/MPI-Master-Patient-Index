package com.mpi.demo.dto.request;

import java.util.Set;

import com.mpi.demo.constant.GenderEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CreateUserRequest {
    @NotBlank(message = "Tên không được để trống")
    @Size(max = 255, message = "Tên tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 255, message = "Mật khẩu từ 6 đến 255 ký tự")
    private String password;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    private GenderEnum gender;

    @Size(max = 500, message = "Avatar tối đa 500 ký tự")
    private String avatar;

    private Set<Long> roleIds;
}
