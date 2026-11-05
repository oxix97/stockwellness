package org.stockwellness.application.port.out.stock;

import java.util.List;
import org.stockwellness.domain.stock.insight.IndexDaily;

public interface IndexDailyPort {
    void upsert(List<IndexDaily> values);
}
