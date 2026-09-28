package com.mpi.demo.service;

import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import com.mpi.demo.constant.FileTypeEnum;

public interface FileStorageService {
    StoredFile store(MultipartFile file);

    InputStream open(String fileToken);

    StoredFile getMetadata(String fileToken);

    FileTypeEnum detectType(String fileName);

    void delete(String fileToken);
}
