package com.codearena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodeArenaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeArenaApplication.class, args);
    }
}
