package com.example.dynamicquery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.dynamicquery.mapper")
public class DynamicQueryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicQueryApplication.class, args);
    }
}