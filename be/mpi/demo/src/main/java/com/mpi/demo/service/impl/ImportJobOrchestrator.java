package com.mpi.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.dto.request.StartImportRequest;
import com.mpi.demo.dto.response.ImportJobResponse;
import com.mpi.demo.service.AsyncImportProcessor;
import com.mpi.demo.service.ImportService;

@Service
public class ImportJobOrchestrator {

    private final ImportService importService;
    private final AsyncImportProcessor asyncImportProcessor;

    public ImportJobOrchestrator(
            ImportService importService,
            AsyncImportProcessor asyncImportProcessor) {
        this.importService = importService;
        this.asyncImportProcessor = asyncImportProcessor;
    }

    @Transactional(propagation = Propagation.NEVER)
    public ImportJobResponse startImportWithAsyncProcessing(StartImportRequest request) {

        ImportJobResponse response = importService.createImportJobOnly(request);
        asyncImportProcessor.process(response.id());
        return response;
    }
}
