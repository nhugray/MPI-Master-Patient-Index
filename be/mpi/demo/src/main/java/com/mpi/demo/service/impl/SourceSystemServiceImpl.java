package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mpi.demo.dto.request.CreateSourceSystemRequest;
import com.mpi.demo.dto.request.SourceSystemSearchRequest;
import com.mpi.demo.dto.request.UpdateSourceSystemRequest;
import com.mpi.demo.dto.response.SourceSystemResponse;
import com.mpi.demo.entity.Facility;
import com.mpi.demo.entity.SourceSystem;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.FacilityRepository;
import com.mpi.demo.repository.SourceSystemRepository;
import com.mpi.demo.service.SourceSystemService;
import com.mpi.demo.specification.SourceSystemSpecification;

@Service
public class SourceSystemServiceImpl implements SourceSystemService {

    private final SourceSystemRepository sourceSystemRepository;
    private final FacilityRepository facilityRepository;

    public SourceSystemServiceImpl(SourceSystemRepository sourceSystemRepository,
            FacilityRepository facilityRepository) {
        this.sourceSystemRepository = sourceSystemRepository;
        this.facilityRepository = facilityRepository;
    }

    @Override
    public ResultPagination search(SourceSystemSearchRequest search, Pageable pageable) {
        Specification<SourceSystem> spec = SourceSystemSpecification.build(search);
        Page<SourceSystemResponse> pageResult = sourceSystemRepository.findBy(spec, q -> q.page(pageable))
                .map(SourceSystemResponse::fromEntity);
        return ResultPagination.fromPage(pageResult);
    }

    @Override
    public SourceSystemResponse getById(Long id) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id", id));
        return SourceSystemResponse.fromEntity(sourceSystem);
    }

    @Override
    public SourceSystemResponse create(CreateSourceSystemRequest request) {
        if (this.sourceSystemRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Hệ thống nguồn", "mã hệ thống", request.getCode());
        }

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Cơ sở y tế", "id", request.getFacilityId()));

        SourceSystem sourceSystem = new SourceSystem();
        sourceSystem.setFacility(facility);
        sourceSystem.setCode(request.getCode());
        sourceSystem.setName(request.getName());
        sourceSystem.setDescription(request.getDescription());
        sourceSystem.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : true);
        SourceSystem saved = sourceSystemRepository.save(sourceSystem);
        return SourceSystemResponse.fromEntity(saved);
    }

    @Override
    public SourceSystemResponse update(UpdateSourceSystemRequest request) {
        SourceSystem sourceSystem = this.sourceSystemRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id", request.getId()));

        if (this.sourceSystemRepository.existsByCode(request.getCode())
                && !sourceSystem.getCode().equals(request.getCode())) {
            throw new DuplicateResourceException("Hệ thống nguồn", "mã hệ thống", request.getCode());
        }

        Facility facility = facilityRepository.findById(request.getFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Cơ sở y tế", "id", request.getFacilityId()));

        sourceSystem.setFacility(facility);
        sourceSystem.setCode(request.getCode());
        sourceSystem.setName(request.getName());
        sourceSystem.setDescription(request.getDescription());
        sourceSystem.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : sourceSystem.getIsActive());
        SourceSystem saved = sourceSystemRepository.save(sourceSystem);
        return SourceSystemResponse.fromEntity(saved);
    }

    @Override
    public void delete(Long id) {
        SourceSystem existingSourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hệ thống nguồn", "id", id));
        sourceSystemRepository.delete(existingSourceSystem);
    }

}
