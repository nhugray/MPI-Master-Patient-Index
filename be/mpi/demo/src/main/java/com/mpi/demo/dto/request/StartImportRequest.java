package com.mpi.demo.dto.request;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class StartImportRequest {
    @NotNull(message = "Id hệ thống nguồn không được để trống")
    private Long sourceSystemId;

    @NotNull(message = "File không được để trống")
    @Size(min = 1, message = "Ít nhất một bản đồ cột phải được chỉ định")
    private Map<String, String> columnMappings;

    @NotNull
    @Builder.Default
    private Boolean skipDuplicates = true;

    @NotNull
    @Min(value = 0, message = "Ngưỡng trùng lặp tối thiểu là 0")
    @Max(value = 100, message = "Ngưỡng trùng lặp tối đa là 100")
    @Builder.Default
    private Integer duplicateThreshold = 70;

    @NotNull
    private String fileToken;

}
