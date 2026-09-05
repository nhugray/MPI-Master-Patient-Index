package com.mpi.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.mpi.demo.constant.FacilityTypeEnum;
import com.mpi.demo.dto.request.CreateFacilityRequest;
import com.mpi.demo.dto.request.FacilitySearchRequest;
import com.mpi.demo.dto.request.UpdateFacilityRequest;
import com.mpi.demo.dto.response.FacilityResponse;
import com.mpi.demo.entity.Facility;
import com.mpi.demo.exception.DuplicateResourceException;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.helper.ResultPagination;
import com.mpi.demo.repository.FacilityRepository;
import com.mpi.demo.service.FacilityService;
import com.mpi.demo.specification.FacilitySpecification;

@Service
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;

    public FacilityServiceImpl(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public ResultPagination search(FacilitySearchRequest search, Pageable pageable) {
        Specification<Facility> spec = FacilitySpecification.build(search);
        Page<FacilityResponse> pageResult = facilityRepository.findBy(spec, q -> q.page(pageable))
                .map(FacilityResponse::fromEntity);
        return ResultPagination.fromPage(pageResult);
    }

    @Override
    public FacilityResponse getById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cơ sở y tế", "id", id));
        return FacilityResponse.fromEntity(facility);
    }

    @Override
    public FacilityResponse create(CreateFacilityRequest request) {
        if (this.facilityRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Cơ sở y tế", "mã cơ sở", request.getCode());
        }
        Facility facility = new Facility();
        facility.setCode(request.getCode());
        facility.setName(request.getName());
        facility.setFacilityType(
                request.getFacilityType() != null ? request.getFacilityType() : FacilityTypeEnum.OTHER);
        facility.setAddress(request.getAddress());
        facility.setPhoneNumber(request.getPhoneNumber());
        facility.setIsActive(
                request.getIsActive() != null ? request.getIsActive() : true);
        Facility saved = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(saved);
    }

    @Override
    public FacilityResponse update(UpdateFacilityRequest request) {
        Facility facility = this.facilityRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cơ sở y tế", "mã cơ sở", request.getId()));

        if (this.facilityRepository.existsByCode(request.getCode())
                && !facility.getCode().equals(request.getCode())) {
            throw new DuplicateResourceException("Cơ sở y tế", "mã cơ sở", request.getCode());
        }
        facility.setCode(request.getCode());
        facility.setName(request.getName());
        facility.setFacilityType(request.getFacilityType() != null ? request.getFacilityType() : facility.getFacilityType());
        facility.setAddress(request.getAddress());
        facility.setPhoneNumber(request.getPhoneNumber());
        facility.setIsActive(request.getIsActive() != null ? request.getIsActive() : facility.getIsActive());
        Facility saved = facilityRepository.save(facility);
        return FacilityResponse.fromEntity(saved);
    }

    @Override
    public void delete(Long id) {
        Facility existingFacility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cơ sở y tế", "mã cơ sở", id));
        facilityRepository.delete(existingFacility);
    }

}
