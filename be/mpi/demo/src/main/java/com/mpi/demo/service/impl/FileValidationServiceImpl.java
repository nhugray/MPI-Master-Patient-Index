package com.mpi.demo.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.mpi.demo.dto.response.FileValidationResponse;
import com.mpi.demo.dto.response.PreviewRowDto;
import com.mpi.demo.dto.response.ValidationErrorDto;
import com.mpi.demo.repository.PatientRepository;
import com.mpi.demo.service.FileValidationService;
import com.mpi.demo.service.ParsedFileData;

@Service
public class FileValidationServiceImpl implements FileValidationService {
    private static final Pattern PHONE = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern NATIONAL_ID = Pattern.compile("^(?:\\d{9}|\\d{12})$");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PatientRepository patientRepository;

    public FileValidationServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public FileValidationResponse validate(ParsedFileData data, Long sourceSystemId) {
        List<ValidationErrorDto> errors = new ArrayList<>();
        List<PreviewRowDto> preview = new ArrayList<>();
        Set<String> localCodes = new HashSet<>();
        Set<String> nationalIds = new HashSet<>();

        Map<String, String> mappings = suggestMappings(data.headers());
        int validRows = 0;
        int invalidRows = 0;

        for (int i = 0; i < data.rows().size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = data.rows().get(i);
            List<String> rowMessages = validateRow(row, mappings, sourceSystemId, localCodes, nationalIds);
            if (rowMessages.isEmpty()) {
                validRows++;
            } else {
                invalidRows++;
                for (String message : rowMessages) {
                    errors.add(new ValidationErrorDto(rowNumber, extractColumnFromMessage(message), message, "ERROR"));
                }
            }
            Map<String, Object> rowDataAsObject = new LinkedHashMap<>(row);

            preview.add(new PreviewRowDto(
                rowNumber,
                rowDataAsObject,
                rowMessages.isEmpty() ? "VALID" : "INVALID",
                rowMessages,
                null
            ));
        }

        return new FileValidationResponse(
                errors.isEmpty(),
                null,
                null,
                null,
                data.rows().size(),
                validRows,
                invalidRows,
                data.headers(),
                mappings,
                errors,
                preview);
    }

    private String value(Map<String, String> row, Map<String, String> mappings, String field) {
        String header = mappings.get(field);
        return row.get(header) == null || header == null ? "" : row.get(header).trim();

    }

    private Map<String, String> suggestMappings(List<String> headers) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, List<String>> aliases = Map.of(
                "localPatientCode", List.of("localPatientCode", "local_code", "patient_code",
                        "ma_benh_nhan"),
                "fullName", List.of("fullName", "full_name", "name", "ho_ten"),
                "dateOfBirth", List.of("dateOfBirth", "dob", "date_of_birth", "ngay_sinh"),
                "nationalId", List.of("nationalId", "national_id", "cccd", "cmnd"),
                "healthInsuranceNo", List.of("healthInsuranceNo", "health_insurance_no",
                        "bhyt"),
                "phoneNumber", List.of("phoneNumber", "phone", "phone_number",
                        "so_dien_thoai"),
                "gender", List.of("gender", "gioi_tinh"),
                "address", List.of("address", "dia_chi"));
        for (Map.Entry<String, List<String>> entry : aliases.entrySet()) {
            headers.stream()
                    .filter(header -> entry.getValue().stream().anyMatch(alias -> alias.equalsIgnoreCase(header)))
                    .findFirst().ifPresent(header -> result.put(entry.getKey(), header));
        }
        return result;
    }

    private List<String> validateRow(Map<String, String> row,
            Map<String, String> mappings,
            Long sourceSystemId,
            Set<String> localCodes,
            Set<String> nationalIds) {
        List<String> messages = new ArrayList<>();
        String localCode = value(row, mappings, "localPatientCode");
        String fullName = value(row, mappings, "fullName");
        String nationalId = value(row, mappings, "nationalId");
        String dateOfBirth = value(row, mappings, "dateOfBirth");
        String phone = value(row, mappings, "phoneNumber");
        String gender = value(row, mappings, "gender");

        if (localCode.isBlank()) {
            messages.add("Thieu localPatientCode");
        } else if (!localCodes.add(localCode)) {
            messages.add("localPatientCode bi trung trong file");
        } else if (patientRepository.existsBySourceSystemIdAndLocalPatientCode(sourceSystemId, localCode)) {
            messages.add("localPatientCode da ton tai trong source system");
        }
        if (fullName.isBlank()) {
            messages.add("Thieu fullName");
        }

        if (!gender.isBlank()) {
            if (!gender.equals("MALE") && !gender.equals("FEMALE") && !gender.equals("OTHER")) {
                messages.add("gender phai la MALE, FEMALE hoac OTHER");
            }
        }

        if (!nationalId.isBlank()) {
            if (!NATIONAL_ID.matcher(nationalId).matches()) {
                messages.add("nationalId phai co 9 hoac 12 chu so");
            } else if (!nationalIds.add(nationalId)) {
                messages.add("nationalId bi trung trong file");
            }
        }
        if (!dateOfBirth.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(dateOfBirth, DATE_FORMAT);
                if (date.isAfter(LocalDate.now())) {
                    messages.add("dateOfBirth khong duoc o tuong lai");
                }
            } catch (DateTimeParseException exception) {
                messages.add("dateOfBirth phai co dinh dang dd/MM/yyyy");
            }
        }
        if (!phone.isBlank() && !PHONE.matcher(phone).matches()) {
            messages.add("phoneNumber khong dung dinh dang");
        }
        return messages;
    }

    private String extractColumnFromMessage(String message) {
        if (message.contains("localPatientCode")) return "localPatientCode";
        if (message.contains("fullName")) return "fullName";
        if (message.contains("dateOfBirth")) return "dateOfBirth";
        if (message.contains("nationalId")) return "nationalId";
        if (message.contains("phoneNumber")) return "phoneNumber";
        if (message.contains("healthInsuranceNo")) return "healthInsuranceNo";
        if (message.contains("gender")) return "gender";
        return "general";
    }

}
