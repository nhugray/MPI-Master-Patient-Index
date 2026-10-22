package com.mpi.demo.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import com.mpi.demo.constant.FileTypeEnum;
import com.mpi.demo.exception.FileParsingException;
import com.mpi.demo.service.FileParserService;
import com.mpi.demo.service.ParsedFileData;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVParserBuilder;
import com.opencsv.exceptions.CsvValidationException;

@Service
public class FileParserServiceImpl implements FileParserService {
    private static final int MAX_ROWS = 500_000;

    @Override
    public ParsedFileData parse(Path path, FileTypeEnum fileType) throws IOException {
        if (fileType == FileTypeEnum.CSV) {
            return parseCsv(path);
        }
        if (fileType == FileTypeEnum.XLSX) {
            return parseXlsx(path);
        }
        throw new FileParsingException("Loại file không được hỗ trợ");
    }

    private ParsedFileData parseCsv(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser(new CSVParserBuilder()
                        .withSeparator(',')
                        .withQuoteChar('"')
                        .withEscapeChar('\\')
                        .withStrictQuotes(false)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build())
                    .build()) {
            String[] rawHeaders = csvReader.readNext();
            if (rawHeaders == null || rawHeaders.length == 0) {
                throw new FileParsingException("File CSV không có dữ liệu");
            }
            List<String> headers = normalizeHeaders(List.of(rawHeaders));
            List<Map<String, String>> rows = new ArrayList<>();
            String[] values;
            int rowNumber = 1;            while ((values = csvReader.readNext()) != null) {
                rowNumber++;
                if (rowNumber > MAX_ROWS) {
                    throw new FileParsingException("File CSV vượt quá giới hạn tối đa " + MAX_ROWS + " dòng.");
                }
                rows.add(toRow(headers, values));
            }
            return new ParsedFileData(headers, rows, FileTypeEnum.CSV);

        } catch (CsvValidationException exception) {
            throw new FileParsingException("CSV không đúng định dạng", exception);
        }
    }

    private ParsedFileData parseXlsx(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
                Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new FileParsingException("File Excel không có dữ liệu");
            }
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() + 1 > MAX_ROWS) {
                throw new FileParsingException("File Excel vượt quá giới hạn tối đa " + MAX_ROWS + " dòng.");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new FileParsingException("File Excel không có dữ liệu");
            }
            DataFormatter dataFormatter = new DataFormatter();
            List<String> rawHeaders = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                rawHeaders.add(dataFormatter.formatCellValue(headerRow.getCell(i)));
            }
            List<String> headers = normalizeHeaders(rawHeaders);
            List<Map<String, String>> rows = new ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    rows.add(toRow(headers, row, dataFormatter));
                }
            }
            return new ParsedFileData(headers, rows, FileTypeEnum.XLSX);
        } catch (FileParsingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new FileParsingException("Không thể đọc file Excel", exception);
        }
    }

    private List<String> normalizeHeaders(List<String> rawHeaders) {
        List<String> headers = new ArrayList<>();
        for (String rawHeader : rawHeaders) {
            String header = rawHeader == null ? "" : rawHeader.replace("\uFEFF", "").trim();
            if (header.isBlank()) {
                throw new FileParsingException("Tiêu đề không được để trống.");
            }
            if (headers.contains(header)) {
                throw new FileParsingException("Tiêu đề bị trùng.");
            }
            headers.add(header);
        }
        return headers;
    }

    private Map<String, String> toRow(List<String> headers, String[] values) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String value = i < values.length ? values[i].trim() : "";
            row.put(headers.get(i), value);
        }
        return row;
    }

    private Map<String, String> toRow(List<String> headers, Row row, DataFormatter dataFormatter) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            String value = cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
            result.put(headers.get(i), value);
        }
        return result;
    }
}
