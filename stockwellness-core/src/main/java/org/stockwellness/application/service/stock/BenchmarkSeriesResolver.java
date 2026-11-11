package org.stockwellness.application.service.stock;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.domain.stock.exception.UnsupportedBenchmarkSeriesException;
import org.stockwellness.domain.stock.insight.IndexSeries;

@Service
@RequiredArgsConstructor
public class BenchmarkSeriesResolver {
    private final IndexSeriesRepository repository;

    public IndexSeries resolve(String publicCode, String returnBasis, String dividendBasis, String currency) {
        if (blank(publicCode) || blank(returnBasis) || blank(dividendBasis) || blank(currency)) {
            throw new UnsupportedBenchmarkSeriesException();
        }
        List<IndexSeries> canonical = repository.findCanonical(
                publicCode, returnBasis, dividendBasis, currency);
        if (canonical.size() != 1) throw new UnsupportedBenchmarkSeriesException();
        return canonical.getFirst();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
