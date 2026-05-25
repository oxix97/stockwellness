package org.stockwellness.support;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseCleaner {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private javax.sql.DataSource dataSource;

    private List<String> tableNames;

    @PostConstruct
    public void findTableNames() {
        tableNames = entityManager.getMetamodel().getEntities().stream()
                .filter(e -> e.getJavaType().getAnnotation(Entity.class) != null)
                .map(e -> {
                    Table table = e.getJavaType().getAnnotation(Table.class);
                    return (table != null && !table.name().isEmpty()) ? table.name() : convertToSnakeCase(e.getName());
                })
                .distinct()
                .collect(Collectors.toList());
        
        if (tableNames.isEmpty()) {
            System.out.println("[DatabaseCleaner] WARN: No entities found for cleaning!");
        } else {
            System.out.println("[DatabaseCleaner] Found " + tableNames.size() + " tables for cleaning: " + tableNames);
        }
    }

    private String convertToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        entityManager.clear();
        
        if (tableNames == null || tableNames.isEmpty()) {
            return;
        }

        boolean isH2 = isH2Database();
        
        if (isH2) {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            for (String tableName : tableNames) {
                try {
                    entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
                    // ID 초기화는 테이블마다 컬럼명이 다를 수 있으므로 생략하거나 더 안전하게 처리
                } catch (Exception e) {
                    System.err.println("[DatabaseCleaner] Failed to truncate table " + tableName + ": " + e.getMessage());
                }
            }
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        } else {
            String tableNamesJoined = String.join(", ", tableNames);
            entityManager.createNativeQuery("TRUNCATE TABLE " + tableNamesJoined + " RESTART IDENTITY CASCADE").executeUpdate();
        }
    }

    private boolean isH2Database() {
        try (java.sql.Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2");
        } catch (Exception e) {
            return false;
        }
    }
}
