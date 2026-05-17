package com.adoptar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdoptarApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdoptarApplication.class, args);
    }
}
