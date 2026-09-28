package com.mpi.demo.service.impl;

import org.springframework.stereotype.Service;

import com.mpi.demo.service.AsyncImportProcessor;

@Service
public class AsyncImportProcessorImpl implements AsyncImportProcessor {
    @Override
    public void process(Long importJobId) {
        // Implementation of the asynchronous import processing logic goes here
    }

}
