package com.mpi.demo.service;

import java.nio.file.Path;

import com.mpi.demo.constant.FileTypeEnum;

public record StoredFile(
        String token,
        String originalFileName,
        long size,
        FileTypeEnum fileType,
        Path path) {
}