package com.mpi.demo.service;

import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreateFacilityRequest;
import com.mpi.demo.dto.request.FacilitySearchRequest;
import com.mpi.demo.dto.request.UpdateFacilityRequest;
import com.mpi.demo.dto.response.FacilityResponse;
import com.mpi.demo.helper.ResultPagination;

public interface FacilityService {

    FacilityResponse getById(Long id);

    FacilityResponse create(CreateFacilityRequest request);

    FacilityResponse update(UpdateFacilityRequest request);

    void delete(Long id);

    ResultPagination search(FacilitySearchRequest search, Pageable pageable);
}
