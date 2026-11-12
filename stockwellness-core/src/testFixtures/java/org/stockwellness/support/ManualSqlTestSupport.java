package org.stockwellness.support;

import java.io.IOException;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;

/** Executes test SQL directly through JDBC; no schema migration tool is involved. */
public final class ManualSqlTestSupport {
    private ManualSqlTestSupport() {}

    public static void execute(DataSource dataSource, String classpathResource) {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var input = new ClassPathResource(classpathResource).getInputStream()) {
            statement.execute(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException | java.sql.SQLException e) {
            throw new IllegalStateException("Failed to execute test SQL resource: " + classpathResource, e);
        }
    }

    public static void initializeLegacyPostgres(DataSource dataSource) {
        execute(dataSource, "db/test/legacy-schema.sql");
        execute(dataSource, "db/test/legacy-series-schema.sql");
        execute(dataSource, "db/test/legacy-price-schema.sql");
        execute(dataSource, "db/test/batch-sequences.sql");
    }
}
