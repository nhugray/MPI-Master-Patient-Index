package com.mpi.demo.service;

import java.io.IOException;
import java.nio.file.Path;

import com.mpi.demo.constant.FileTypeEnum;

public interface FileParserService {
    ParsedFileData parse(Path path, FileTypeEnum fileType) throws IOException;
}
