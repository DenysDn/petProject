package com.company.TgBot.entity;


import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity(name = "Sites")
public class SiteSushiDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String url;
    private String productInList;
    private String hrefSelect;
    private String urlSite;
    private String nameItem;
    private String priceItem;
    private String weightItem;

    public SiteSushiDetails(String url, String productInList, String hrefSelect, String urlSite, String nameItem, String priceItem, String weightItem) {
        this.url = url;
        this.productInList = productInList;
        this.hrefSelect = hrefSelect;
        this.urlSite = urlSite;
        this.nameItem = nameItem;
        this.priceItem = priceItem;
        this.weightItem = weightItem;
    }

    public SiteSushiDetails() {
    }
}
