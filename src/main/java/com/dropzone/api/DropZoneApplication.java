package com.dropzone.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DropZoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(DropZoneApplication.class, args);
    }

}