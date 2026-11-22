package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionTest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String database = jdbcTemplate.queryForObject(
                "SELECT DB_NAME()",
                String.class);

        System.out.println("Ket noi SQL Server thanh cong!");
        System.out.println("Database: " + database);
    }
}
