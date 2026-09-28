package com.mpi.demo.dto.request;

import org.springframework.web.multipart.MultipartFile;

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
public class UploadFileRequest {
    @NotNull(message = "File không được để trống")
    @Size(max = 524288000, message = "Kích thước file tối đa 500MB")
    private MultipartFile file;

    @NotNull (message = "Id hệ thống nguồn không được để trống")
    private Long sourceSystemId;
}
