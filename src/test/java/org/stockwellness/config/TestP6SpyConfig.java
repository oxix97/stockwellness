package org.stockwellness.config;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.P6SpyOptions;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import jakarta.annotation.PostConstruct;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Locale;

@TestConfiguration
public class TestP6SpyConfig {

    @PostConstruct
    public void setLogMessageFormat() {
        // P6Spy가 이미 초기화된 이후에 포맷터를 강제로 덮어씌웁니다.
        P6SpyOptions.getActiveInstance().setLogMessageFormat(P6SpyFormatter.class.getName());
    }

    @Bean
    public MessageFormattingStrategy messageFormattingStrategy() {
        return new P6SpyFormatter();
    }

    // 람다 대신 static class로 정의해야 P6SpyOptions에 클래스 이름으로 등록할 수 있습니다.
    public static class P6SpyFormatter implements MessageFormattingStrategy {

        @Override
        public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
            sql = formatSql(category, sql);
            // [출력 포맷] | 시간ms | SQL
            return String.format("[%s] | %d ms | %s", category, elapsed, sql);
        }

        private String formatSql(String category, String sql) {
            if (sql == null || sql.trim().isEmpty()) return sql;

            // Only format Statement, not ResultSet etc.
            if (Category.STATEMENT.getName().equals(category)) {
                String tmpsql = sql.trim().toLowerCase(Locale.ROOT);
                if (tmpsql.startsWith("create") || tmpsql.startsWith("alter") || tmpsql.startsWith("comment")) {
                    sql = FormatStyle.DDL.getFormatter().format(sql);
                } else {
                    sql = FormatStyle.BASIC.getFormatter().format(sql);
                }
            }
            return sql;
        }
    }
}