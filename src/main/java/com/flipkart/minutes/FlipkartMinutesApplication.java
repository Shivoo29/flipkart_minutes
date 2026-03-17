package com.flipkart.minutes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Flipkart Minutes – Quick Commerce System
 *
 * A standalone Spring Boot application (no HTTP server) that simulates
 * Flipkart's instant delivery platform. Manages customers, delivery partners,
 * inventory, and orders with thread-safe concurrency.
 *
 * Entry point: DemoRunner (CommandLineRunner) executes all test scenarios.
 */
@SpringBootApplication
@EnableScheduling
public class FlipkartMinutesApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FlipkartMinutesApplication.class);
        // Standalone application — no embedded web server needed
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
