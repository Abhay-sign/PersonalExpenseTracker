package com.abhay.expensetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;
/**
 * Entry point. @SpringBootApplication turns on:
 *  - component scanning (finds @RestController, @Service, @Repository in this package and below)
 *  - auto-configuration (sees postgres + JPA on the classpath and wires a DataSource, EntityManager, etc.)
 */
@SpringBootApplication
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        // Run the whole backend in UTC so it doesn't depend on the machine's timezone
        // (Windows reports IST as the legacy "Asia/Calcutta", which Postgres rejects).
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}
