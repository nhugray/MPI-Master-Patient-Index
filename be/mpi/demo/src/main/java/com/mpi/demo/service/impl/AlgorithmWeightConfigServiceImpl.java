package com.mpi.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.dto.request.CreateWeightConfigRequest;
import com.mpi.demo.dto.response.WeightConfigResponse;
import com.mpi.demo.entity.AlgorithmWeightConfig;
import com.mpi.demo.exception.ResourceNotFoundException;
import com.mpi.demo.repository.AlgorithmWeightConfigRepository;
import com.mpi.demo.service.AlgorithmWeightConfigService;

@Service
public class AlgorithmWeightConfigServiceImpl implements AlgorithmWeightConfigService {

    private static final BigDecimal TOTAL_WEIGHT_EXPECTED = new BigDecimal("100.00");
    private static final BigDecimal WEIGHT_TOLERANCE = new BigDecimal("0.01");

    private final AlgorithmWeightConfigRepository algorithmWeightConfigRepository;

    public AlgorithmWeightConfigServiceImpl(AlgorithmWeightConfigRepository algorithmWeightConfigRepository) {
        this.algorithmWeightConfigRepository = algorithmWeightConfigRepository;
    }

    @Override
    public WeightConfigResponse getActiveConfig() {
        AlgorithmWeightConfig config = algorithmWeightConfigRepository.findByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình trọng số", "isActive", true));
        return WeightConfigResponse.fromEntity(config);
    }

    @Override
    public Page<WeightConfigResponse> getAllConfigs(Pageable pageable) {
        return algorithmWeightConfigRepository.findAll(pageable)
                .map(WeightConfigResponse::fromEntity);
    }

    @Override
    public WeightConfigResponse getConfigById(Long id) {
        AlgorithmWeightConfig config = algorithmWeightConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình trọng số", "id", id));
        return WeightConfigResponse.fromEntity(config);
    }

    @Override
    @Transactional
    public WeightConfigResponse createConfig(CreateWeightConfigRequest request) {
        validateTotalWeight(request);

        AlgorithmWeightConfig config = AlgorithmWeightConfig.builder()
                .version(generateNextVersion())
                .nameWeight(request.nameWeight())
                .dobWeight(request.dobWeight())
                .nationalIdWeight(request.nationalIdWeight())
                .phoneWeight(request.phoneWeight())
                .addressWeight(request.addressWeight())
                .autoApprovalThreshold(request.autoApprovalThreshold())
                .manualReviewThreshold(request.manualReviewThreshold())
                .description(request.description())
                .createdBy(request.createdBy())
                .isActive(false)
                .build();

        AlgorithmWeightConfig saved = algorithmWeightConfigRepository.save(config);
        algorithmWeightConfigRepository.findByIsActiveTrue().ifPresent(current -> {
            current.setIsActive(false);
            algorithmWeightConfigRepository.save(current);
        });
        saved.setIsActive(true);
        return WeightConfigResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public WeightConfigResponse activateConfig(Long id) {
        AlgorithmWeightConfig config = algorithmWeightConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình trọng số", "id", id));

        algorithmWeightConfigRepository.findByIsActiveTrue().ifPresent(current -> {
            current.setIsActive(false);
            algorithmWeightConfigRepository.save(current);
        });

        config.setIsActive(true);
        config.setDeployedAt(LocalDateTime.now());
        AlgorithmWeightConfig saved = algorithmWeightConfigRepository.save(config);

        return WeightConfigResponse.fromEntity(saved);
    }

    private void validateTotalWeight(CreateWeightConfigRequest request) {
        BigDecimal total = request.nameWeight()
                .add(request.dobWeight())
                .add(request.nationalIdWeight())
                .add(request.phoneWeight())
                .add(request.addressWeight());

        BigDecimal diff = total.subtract(TOTAL_WEIGHT_EXPECTED).abs();
        if (diff.compareTo(WEIGHT_TOLERANCE) > 0) {
            throw new IllegalArgumentException(
                    String.format("Tổng trọng số phải bằng 100%%, hiện tại là %s%%", total));
        }
    }

    private String generateNextVersion() {
        long count = algorithmWeightConfigRepository.count();
        int major = 2;
        int minor = (int) count + 1;
        String candidate = String.format("v%d.%d", major, minor);

        while (algorithmWeightConfigRepository.existsByVersion(candidate)) {
            minor++;
            candidate = String.format("v%d.%d", major, minor);
        }
        return candidate;
    }
}
