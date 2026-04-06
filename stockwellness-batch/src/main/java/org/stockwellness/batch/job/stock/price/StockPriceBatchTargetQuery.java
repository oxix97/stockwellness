package org.stockwellness.batch.job.stock.price;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StockPriceBatchTargetQuery {

    private StockPriceBatchTargetQuery() {
    }

    public static String selectQuery(String targetTicker) {
        // Reader와 count 계산이 완전히 같은 조건을 쓰도록 query 조합을 한 곳으로 모은다.
        return baseQuery(false, targetTicker) + " ORDER BY s.id ASC";
    }

    public static String countQuery(String targetTicker) {
        return baseQuery(true, targetTicker);
    }

    public static Map<String, Object> parameters(String targetTicker) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (StringUtils.hasText(targetTicker)) {
            parameters.put("targetTicker", targetTicker);
        }
        return parameters;
    }

    private static String baseQuery(boolean countOnly, String targetTicker) {
        String selectClause = countOnly ? "SELECT COUNT(s) " : "SELECT s ";
        StringBuilder query = new StringBuilder(selectClause)
                .append("FROM Stock s ")
                .append("WHERE s.status = 'ACTIVE' ")
                .append("AND s.marketType IN ('KOSPI', 'KOSDAQ') ")
                .append("AND s.groupCode IN ('ST', 'EF', 'EN') ")
                .append("AND s.ticker BETWEEN '000000' AND '999999'");

        if (StringUtils.hasText(targetTicker)) {
            query.append(" AND s.ticker = :targetTicker");
        }

        return query.toString();
    }
}
