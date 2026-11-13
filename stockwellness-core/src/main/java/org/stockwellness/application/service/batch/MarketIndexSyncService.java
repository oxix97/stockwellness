package org.stockwellness.application.service.batch;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.application.parser.MarketIndexMstParser;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.insight.MarketIndex;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexSyncService {

    private static final String OVERSEAS_INDEX_NAMESPACE = "kis.ovrs.idxcode.N";
    private static final List<OverseasBenchmark> KIS_US_BENCHMARKS = List.of(
            new OverseasBenchmark("US:SPX", "S&P 500", "SPX", "NYSE"),
            new OverseasBenchmark("US:NDX", "NASDAQ 100", "NDX", "NASDAQ"),
            new OverseasBenchmark("US:DJI", "Dow Jones Industrial", ".DJI", "NYSE"));

    private final MarketIndexRepository marketIndexRepository;
    private final IndexSeriesRepository indexSeriesRepository;

    @Transactional
    public void syncIndices(String fileName) throws IOException {
        Path path = Paths.get("kis", fileName);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("업종 마스터 파일을 찾을 수 없습니다: " + path);
        }

        log.info("[MarketIndex] 동기화 시작: {}", fileName);
        List<MarketIndex> parsedIndices = MarketIndexMstParser.parse(path);
        log.info("[MarketIndex] 파싱 완료: {}건", parsedIndices.size());

        // 기존 데이터 로드 (Upsert 처리를 위함)
        Map<String, MarketIndex> existingMap = marketIndexRepository.findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexKey, m -> m, (m1, m2) -> m1));

        int newCount = 0;
        int updateCount = 0;

        for (MarketIndex parsed : parsedIndices) {
            MarketIndex existing = existingMap.get(parsed.getIndexKey());
            MarketIndex persisted;
            if (existing != null) {
                if (existing.updateFrom(parsed)) {
                    updateCount++;
                }
                persisted = existing;
            } else {
                persisted = marketIndexRepository.save(parsed);
                newCount++;
            }
            if (parsed.getMarketCode() != null) {
                indexSeriesRepository.findByProviderAndProviderNamespaceAndProviderSeriesCodeAndReturnBasisAndDividendBasisAndCurrency(
                        parsed.getProvider(), "kis.idxcode." + parsed.getMarketCode(), parsed.getProviderCode(),
                        "PRICE", "NONE", "KRW")
                        .orElseGet(() -> indexSeriesRepository.save(new IndexSeries(
                                persisted, parsed.getProvider(), "kis.idxcode." + parsed.getMarketCode(), parsed.getProviderCode(),
                                parsed.getMarketCode(), "PRICE", "NONE", "KRW", true)));
            }
        }
        syncOverseasBenchmarks();

        log.info("[MarketIndex] 동기화 완료: 신규 {}건, 기존 {}건", newCount, existingMap.size());
    }

    private void syncOverseasBenchmarks() {
        for (OverseasBenchmark benchmark : KIS_US_BENCHMARKS) {
            MarketIndex desired = MarketIndex.ofOverseasBenchmark(
                    benchmark.indexKey(), benchmark.name(), benchmark.code());
            MarketIndex index = marketIndexRepository.findByIndexKey(benchmark.indexKey())
                    .map(existing -> {
                        existing.updateFrom(desired);
                        return existing;
                    })
                    .orElseGet(() -> marketIndexRepository.save(desired));
            indexSeriesRepository.findByProviderAndProviderNamespaceAndProviderSeriesCodeAndReturnBasisAndDividendBasisAndCurrency(
                    "KIS", OVERSEAS_INDEX_NAMESPACE, benchmark.code(), "PRICE", "NONE", "USD")
                    .orElseGet(() -> indexSeriesRepository.save(new IndexSeries(
                            index, "KIS", OVERSEAS_INDEX_NAMESPACE, benchmark.code(), benchmark.calendarMarketCode(),
                            "PRICE", "NONE", "USD", true)));
        }
    }

    private record OverseasBenchmark(String indexKey, String name, String code, String calendarMarketCode) {}
}
