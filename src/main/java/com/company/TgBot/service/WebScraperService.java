package com.company.TgBot.service;


import com.company.TgBot.entity.Product;
import com.company.TgBot.entity.SiteSushiDetails;
import com.company.TgBot.repository.SiteSushiRepository;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class WebScraperService {
    private final CacheManager cacheManager;
    private final SiteSushiRepository siteSushiRepository;

    public WebScraperService(CacheManager cacheManager, SiteSushiRepository siteSushiRepository) {
        this.cacheManager = cacheManager;
        this.siteSushiRepository = siteSushiRepository;
    }

    @Cacheable(value = "cacheSushi", unless = "#result == null")
    public List<Product> getSushi() {
        Cache cache = cacheManager.getCache("cacheSushi");
        List<Product> listSushi = new ArrayList<>();
        if (cache == null) {
            if (siteSushiRepository.count() == 0) {
                addSitesToDB();
            }
           listSushi = fetchData(siteSushiRepository.findAll());
        }

        Cache.ValueWrapper valueWrapper = Objects.requireNonNull(cache).get("sushiData");

        if (valueWrapper != null) {
            Object cachedValue = valueWrapper.get();

            if (cachedValue instanceof List) {
                List<?> cachedList = (List<?>) cachedValue;
                listSushi = new ArrayList<>(cachedList.size());

                for (Object item : cachedList) {
                    if (item instanceof Product) {
                        listSushi.add((Product) item);
                    }
                }
            }
        }

        if (listSushi.isEmpty()) {
            if (siteSushiRepository.count() == 0) {
                addSitesToDB();
            }
            listSushi = fetchData(siteSushiRepository.findAll());
            cache.put("sushiData", listSushi);
        }
        return listSushi;
    }

    private List<Product> fetchData(Iterable<SiteSushiDetails> siteList) {
        List<Product> listSushi = new ArrayList<>();
        for (SiteSushiDetails site : siteList) {
            try {
                String link = site.getUrl();
                Document document = Jsoup.connect(link).get();

                // Finding elements with the required data
                Elements products = document.select(site.getProductInList());

                for (Element product : products) {
                    Product newSushi = new Product();
                    if (checkToNullFieldSiteWithSushi(site)) {
                        if (site.getHrefSelect() != null) {
                            String href = product.select(site.getHrefSelect()).attr("href");
                            String fullHref = site.getUrlSite() + href;
                            newSushi.setLink(fullHref);

                        } else {
                            String href = product.select(site.getUrlSite()).attr("href");
                            newSushi.setLink(href);
                        }
                        newSushi.setName(product.select(site.getNameItem()).text());
                        newSushi.setPrice(Objects.requireNonNull(product.select(site.getPriceItem()).last()).text());

                        newSushi.setWeight(extractWeight(product.select(site.getWeightItem()).text()));
                        listSushi.add(newSushi);
                    }
                }
            } catch (Exception e) {
                log.error("Error connect : " + e.getMessage());
            }
        }
        return listSushi;
    }

    private void addSitesToDB() {
        List<SiteSushiDetails> siteWithSushiList = new ArrayList<>();
        SiteSushiDetails sushiMaster = new SiteSushiDetails("https://kharkiv.sushi-master.ua/menu/nabory",
                ".products-list-el", ".title.pointer", "https://kharkiv.sushi-master.ua",
                ".title.pointer", ".current-price.new-price",
                ".under-title.flex.align-center.justify-between.full-width");
        siteWithSushiList.add(sushiMaster);

        SiteSushiDetails rollClub = new SiteSushiDetails("https://roll-club.kh.ua/tovar-category/nabory/",
                ".product-content", null, ".product-link", ".product-title",
                ".price-simple .amount", ".product-checksize");
        siteWithSushiList.add(rollClub);

        SiteSushiDetails kingPizza = new SiteSushiDetails("https://kingpizza.kh.ua/zakaz-seti", ".product-item-list",
                "a.thumb-link", "https://kingpizza.kh.ua",
                "a.product-name-line", ".item-result", ".seti-description-weight");
        /*siteWithSushiList.add(kingPizza);*/

        SiteSushiDetails sushiPapa = new SiteSushiDetails("https://sushipapa.com.ua/collection/sety",
                ".product-card", ".product-link", "https://sushipapa.com.ua",
                ".product-link", ".price", ".product-introtext");
        siteWithSushiList.add(sushiPapa);

        siteSushiRepository.deleteAll();
        siteSushiRepository.saveAll(siteWithSushiList);
    }

    private boolean checkToNullFieldSiteWithSushi(SiteSushiDetails sushiWithSushi) {
        return sushiWithSushi.getUrl() != null && sushiWithSushi.getUrlSite() != null &&
                sushiWithSushi.getPriceItem() != null && sushiWithSushi.getNameItem() != null &&
                sushiWithSushi.getWeightItem() != null && sushiWithSushi.getProductInList() != null;
    }

    private String extractWeight(String text) {
        String weight = text;
        int weightIndex = text.indexOf("Вес:");
        if (weightIndex != -1) {
            int startIndex = weightIndex + 4;
            int endIndex = text.indexOf("грамм", startIndex);
            if (endIndex != -1) {
                weight = text.substring(startIndex, endIndex).trim();
            }
        }
        return weight;
    }
}
