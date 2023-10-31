package com.company.TgBot.service;


import com.company.TgBot.entity.Product;
import com.company.TgBot.repository.SiteSushiRepository;
import  org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebScraperServiceTest {

    @InjectMocks
    private WebScraperService webScraperService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private SiteSushiRepository siteSushiRepository;

    @BeforeEach
    public void setUp() {
        when(cacheManager.getCache("cacheSushi")).thenReturn(cache);
    }

    @Test
    public void testGetSushiWithCachedData() {
        List<Product> cachedProducts = new ArrayList<>();

        Cache.ValueWrapper valueWrapper = mock(Cache.ValueWrapper.class);
        when(cache.get("sushiData")).thenReturn(valueWrapper);
        when(valueWrapper.get()).thenReturn(cachedProducts);


        List<Product> result = webScraperService.getSushi();

        assertEquals(cachedProducts, result);
    }

    @Test
    public void testGetSushiWithEmptyCacheAndEmptyRepository() {
        when(siteSushiRepository.findAll()).thenReturn(new ArrayList<>());

        List<Product> result = webScraperService.getSushi();

        assertEquals(0, result.size());
        verify(siteSushiRepository).count();
        verify(siteSushiRepository).saveAll(anyList());
    }

}