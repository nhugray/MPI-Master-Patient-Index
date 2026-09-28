package com.mpi.demo.service;

import java.util.List;
import java.util.Map;

import com.mpi.demo.constant.FileTypeEnum;

public record ParsedFileData(
        List<String> headers,
        List<Map<String, String>> rows,
        FileTypeEnum fileType) {
}
