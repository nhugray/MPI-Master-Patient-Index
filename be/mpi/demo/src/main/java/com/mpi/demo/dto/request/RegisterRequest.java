package com.mpi.demo.dto.request;

import com.mpi.demo.constant.GenderEnum;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Tên không được để trống") @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự") String name,

        @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email,

        @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 100, message = "Mật khẩu phải có ít nhất 8 ký tự") String password,

        GenderEnum gender,

        @Size(max = 255, message = "Địa chỉ không được quá 255 ký tự") String address) {
}
