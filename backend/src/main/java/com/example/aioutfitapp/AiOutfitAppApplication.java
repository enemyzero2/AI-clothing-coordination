package com.example.aioutfitapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * AI衣搭应用后端主类
 * 
 * 应用程序入口点，SpringBoot启动类
 */
@SpringBootApplication
@EnableJpaAuditing
public class AiOutfitAppApplication {

    /**
     * 应用程序主方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AiOutfitAppApplication.class, args);
    }
} 