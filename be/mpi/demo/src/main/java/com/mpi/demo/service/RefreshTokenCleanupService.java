package com.mpi.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mpi.demo.repository.RefreshTokenRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor 
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 2 * * ?") 
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens...");
        
        int deletedCount = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
        
        log.info("Cleaned up {} expired refresh tokens successfully.", deletedCount);
    }
}

