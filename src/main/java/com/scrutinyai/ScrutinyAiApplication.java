package com.scrutinyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class ScrutinyAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScrutinyAiApplication.class, args);
    }
}
