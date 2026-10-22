package com.mpi.demo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.exception.FileStorageException;
import com.mpi.demo.service.FileStorageService;
import com.mpi.demo.service.StoredFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path uploadDirectory;
    private final long maxFileSize;
    private final Map<String, StoredFile> metadata = new ConcurrentHashMap<>();

    public FileStorageServiceImpl(
            @Value("${file.upload.dir}") String uploadDirectory,
            @Value("${file.upload.max-size}") long maxFileSize) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        try {
            Files.createDirectories(this.uploadDirectory);
        } catch (IOException exception) {
            throw new RuntimeException("Không thệ tạo thư mục lưu file", exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File không được để trống");
        }

        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File vượt quá giới hạn kích thước cho phép");
        }
        String originalName = StringUtils
                .cleanPath(file.getOriginalFilename() == null ? "import" : file.getOriginalFilename());
        FileTypeEnum type = detectType(originalName);
        String token = UUID.randomUUID().toString();
        Path target = uploadDirectory.resolve(token + extension(type)).normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new FileStorageException("Duong dan file khong hop le");
        }
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        StoredFile storedFile = new StoredFile(token, originalName, file.getSize(), type, target);
        metadata.put(token, storedFile);
        return storedFile;

    }

    @Override
    public InputStream open(String fileToken) throws IOException {
        return Files.newInputStream(getMetadata(fileToken).path());
    }

    @Override
    public StoredFile getMetadata(String fileToken) {
        StoredFile storedFile = metadata.get(fileToken);
        if (storedFile == null) {
            throw new FileStorageException("Không tìm thấy file với token: " + fileToken);
        }
        if (!Files.isRegularFile(storedFile.path())) {
            throw new FileStorageException("File đã bị xóa hoặc không tồn tại: " + fileToken);
        }
        return storedFile;
    }

    @Override
    public FileTypeEnum detectType(String fileName) {
        String extension = StringUtils.getFilenameExtension(fileName);
        if (extension == null) {
            throw new FileStorageException("File phai co duoi .csv hoac .xlsx");
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "csv" -> FileTypeEnum.CSV;
            case "xlsx" -> FileTypeEnum.XLSX;
            default -> throw new FileStorageException("Chi ho tro file CSV va XLSX");
        };
    }

    @Override
    public void delete(String fileToken) throws IOException {
        StoredFile storedFile = metadata.remove(fileToken);
        if (storedFile != null) {
            Files.deleteIfExists(storedFile.path());
        }
    }

    @Override
    public String extension(FileTypeEnum type) {
        return type == FileTypeEnum.CSV ? ".csv" : ".xlsx";
    }

}
