package org.stockwellness.batch.job.stockpricerepair.support;

import org.stockwellness.global.util.QueryTypeUtil;

public final class StockPriceRepairSql {

    public static final String UPDATE_PREV_CLOSE = String.format(
            "UPDATE stock_price SET prev_close_price = %s WHERE stock_id = %s AND base_date = %s",
            QueryTypeUtil.NUMERIC,
            QueryTypeUtil.BIGINT,
            QueryTypeUtil.DATE
    );

    private StockPriceRepairSql() {
    }
}
