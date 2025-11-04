package com.gym.service.gymmanagementservice;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.TimeZone;

@SpringBootApplication
public class GymManagementServiceApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(GymManagementServiceApplication.class)
                .properties("spring.config.additional-location=optional:classpath:application-secrets.properties")
                .run(args);
    }
}
