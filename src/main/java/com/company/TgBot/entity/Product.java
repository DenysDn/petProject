package com.company.TgBot.entity;


import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class Product {
    private String link;
    private String name;
    private String price;
    private String weight;
}
