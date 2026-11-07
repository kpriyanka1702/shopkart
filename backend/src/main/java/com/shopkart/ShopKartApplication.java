package com.shopkart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ShopKartApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopKartApplication.class, args);
    }
}
