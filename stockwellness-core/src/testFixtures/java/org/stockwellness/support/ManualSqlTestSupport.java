package org.stockwellness.support;

import java.io.IOException;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

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

    public static void initializeRedesignedPostgres(DataSource dataSource) {
        String schema = "db/manual/2026-11-11-redesign/";
        execute(dataSource, schema + "00-preflight.sql");
        for (String file : new String[] {"02-schema.sql", "03-batch-metadata.sql", "04-reference-data.sql"}) {
            try (var connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(schema + file));
            } catch (java.sql.SQLException e) {
                throw new IllegalStateException("Failed to apply test SQL resource: " + schema + file, e);
            }
        }
        execute(dataSource, schema + "05-verify.sql");
    }
}
