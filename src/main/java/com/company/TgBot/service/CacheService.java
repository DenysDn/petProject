package com.company.TgBot.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@Slf4j
public class CacheService {
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Scheduled(fixedRate = 900000) // 900000 миллисекунд (15 минут)
    public void clearCache() {
        log.info("Cleaning CACHE");
        Objects.requireNonNull(cacheManager.getCache("cacheSushi")).clear();
    }
}
