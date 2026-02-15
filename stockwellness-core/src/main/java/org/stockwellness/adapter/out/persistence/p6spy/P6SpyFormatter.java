package org.stockwellness.adapter.out.persistence.p6spy;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.springframework.util.StringUtils;

import java.util.Locale;

public class P6SpyFormatter implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        sql = formatSql(category, sql);
        // 포맷: [실행시간] | 카테고리 | 실행시간(ms) | 포맷팅된 SQL
        return String.format("[%s] | %s | %d ms | %s", now, category, elapsed, sql);
    }

    private String formatSql(String category, String sql) {
        if (!StringUtils.hasText(sql) || !category.equals("statement")) {
            return sql;
        }

        String trimmedSql = sql.trim().toLowerCase(Locale.ROOT);

        // DDL, DML 구분하여 포맷팅 적용
        if (trimmedSql.startsWith("create") || trimmedSql.startsWith("alter") || trimmedSql.startsWith("comment")) {
            return FormatStyle.DDL.getFormatter().format(sql);
        } else {
            return FormatStyle.BASIC.getFormatter().format(sql);
        }
    }
}