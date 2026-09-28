package com.mpi.demo.service.impl;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.service.FileParserService;
import com.mpi.demo.service.ParsedFileData;

@Service
public class FileParserServiceImpl implements FileParserService {

    @Override
    public ParsedFileData parse(Path path, FileTypeEnum fileType) {
        if (fileType == FileTypeEnum.CSV) {
            return null;
        }
        if (fileType == FileTypeEnum.XLSX) {
            return null;
        }
        return null;
    }

}
