package org.stockwellness.application.port.out.external.kis;

import java.time.LocalDate;
import java.util.List;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;

public interface KisHolidayPort {
    List<KisHolidayEntry> fetchHolidays(LocalDate baseDate);
}
