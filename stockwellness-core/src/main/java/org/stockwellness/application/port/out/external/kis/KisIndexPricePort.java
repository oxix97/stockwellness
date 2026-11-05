package org.stockwellness.application.port.out.external.kis;

import java.time.LocalDate;
import java.util.List;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;

public interface KisIndexPricePort {
    List<KisDailySectorDetail> fetchRawIndexDailyPrices(String providerIndexCode, LocalDate startDate, LocalDate endDate);
}
