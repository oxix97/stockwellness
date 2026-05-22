package org.stockwellness.support;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseCleaner {
    @PersistenceContext
    private EntityManager entityManager;

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
    }

    private String convertToSnakeCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Transactional
    public void execute() {
        entityManager.flush();
        entityManager.clear();
        
        if (tableNames.isEmpty()) {
            return;
        }
        
        String tableNamesJoined = String.join(", ", tableNames);
        entityManager.createNativeQuery("TRUNCATE TABLE " + tableNamesJoined + " RESTART IDENTITY CASCADE").executeUpdate();
    }
}
