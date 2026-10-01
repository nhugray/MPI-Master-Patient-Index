package com.mpi.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mpi.demo.dto.request.CreateWeightConfigRequest;
import com.mpi.demo.dto.response.WeightConfigResponse;

public interface AlgorithmWeightConfigService {

    WeightConfigResponse getActiveConfig();

    Page<WeightConfigResponse> getAllConfigs(Pageable pageable);

    WeightConfigResponse getConfigById(Long id);

    WeightConfigResponse createConfig(CreateWeightConfigRequest request);

    WeightConfigResponse activateConfig(Long id);
}
