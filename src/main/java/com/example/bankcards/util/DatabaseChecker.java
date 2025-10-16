package com.example.bankcards.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseChecker implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== ПРОВЕРКА БАЗЫ ДАННЫХ ===");

        try {
            // Проверяем существование таблиц
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            );

            System.out.println("Найдены таблицы:");
            if (tables.isEmpty()) {
                System.out.println("ТАБЛИЦ НЕТ!");
            } else {
                tables.forEach(table ->
                        System.out.println(" - " + table.get("table_name"))
                );
            }

            // Проверяем таблицу Liquibase
            List<Map<String, Object>> changelog = jdbcTemplate.queryForList(
                    "SELECT * FROM databasechangelog LIMIT 5"
            );
            System.out.println("Записи в databasechangelog: " + changelog.size());

        } catch (Exception e) {
            System.out.println("Ошибка при проверке БД: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== КОНЕЦ ПРОВЕРКИ ===");
    }
}