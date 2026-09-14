package com.mpi.demo.service;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreateSourceSystemRequest;
import com.mpi.demo.dto.request.SourceSystemSearchRequest;
import com.mpi.demo.dto.request.UpdateSourceSystemRequest;
import com.mpi.demo.dto.response.SourceSystemResponse;
import com.mpi.demo.helper.ResultPagination;

public interface SourceSystemService {

    SourceSystemResponse getById(Long id);

    SourceSystemResponse create(CreateSourceSystemRequest request);

    SourceSystemResponse update(UpdateSourceSystemRequest request);

    void delete(Long id);

    ResultPagination search(SourceSystemSearchRequest search, Pageable pageable);
}
