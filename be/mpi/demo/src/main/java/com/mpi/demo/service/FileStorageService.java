package com.mpi.demo.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

import com.mpi.demo.constant.FileTypeEnum;

public interface FileStorageService {
    StoredFile store(MultipartFile file) throws IOException;

    InputStream open(String fileToken) throws IOException;

    StoredFile getMetadata(String fileToken);

    FileTypeEnum detectType(String fileName);

    void delete(String fileToken) throws IOException;

    String extension(FileTypeEnum type);

}
