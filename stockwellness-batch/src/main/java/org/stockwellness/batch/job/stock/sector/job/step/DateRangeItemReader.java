package org.stockwellness.batch.job.stock.sector.job.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import org.stockwellness.global.util.DateUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
public class DateRangeItemReader implements ItemReader<LocalDate> {

    private Iterator<LocalDate> dateIterator;

    public DateRangeItemReader(
            @Value("#{jobParameters['startDate']}") String startDateStr,
            @Value("#{jobParameters['endDate']}") String endDateStr
    ) {
        LocalDate start = StringUtils.hasText(startDateStr) ? DateUtil.parse(startDateStr) : DateUtil.today();
        LocalDate end = StringUtils.hasText(endDateStr) ? DateUtil.parse(endDateStr) : start;

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        this.dateIterator = dates.iterator();
        log.info("DateRangeItemReader initialized: {} to {} (Total: {} days)", start, end, dates.size());
    }

    @Override
    public LocalDate read() {
        return (dateIterator.hasNext()) ? dateIterator.next() : null;
    }
}
