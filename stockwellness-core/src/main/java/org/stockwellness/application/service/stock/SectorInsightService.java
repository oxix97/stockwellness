package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.stock.SectorInsightUseCase;
import org.stockwellness.application.port.in.stock.result.SectorComparisonResult;
import org.stockwellness.application.port.in.stock.result.SectorDetailResult;
import org.stockwellness.application.port.in.stock.result.SectorRankingResult;
import org.stockwellness.application.port.out.stock.*;
import org.stockwellness.domain.stock.MarketType;
import org.stockwellness.domain.stock.insight.SectorInsight;
import org.stockwellness.domain.stock.insight.exception.SectorDomainException;
import org.stockwellness.domain.stock.analysis.DiagnosisStatus;
import org.stockwellness.domain.stock.price.BenchmarkPrice;
import org.stockwellness.domain.stock.price.TechnicalIndicators;
import org.stockwellness.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SectorInsightService implements SectorInsightUseCase {

    private final SectorInsightPort sectorInsightPort;
    private final BenchmarkPricePort benchmarkPricePort;

    @Override
    @Transactional
    public void syncAllSectorInsights() {
        log.info("섹터 동기화는 이제 Batch Job(sectorEodJob)을 통해 관리됩니다. 이 메서드는 더 이상 사용되지 않습니다.");
    }

    @Override
    @Cacheable(cacheNames = "sectorRanking", key = "#date.toString() + '_' + (#marketType != null ? #marketType.name() : 'ALL') + '_' + #limit + '_v2'")
    public List<SectorRankingResult> getTopSectorsByFluctuation(LocalDate date, MarketType marketType, int limit) {
        // 지정된 날짜의 전체 섹터 조회 (KOSPI/KOSDAQ 동시 조회 시 이름 중복 해결을 위해)
        List<SectorInsight> allInsights = sectorInsightPort.findAllByDate(date, marketType);

        if (allInsights.isEmpty()) {
            allInsights = sectorInsightPort.findLatestDate()
                    .map(latestDate -> sectorInsightPort.findAllByDate(latestDate, marketType))
                    .orElse(List.of());
        }

        if (allInsights.isEmpty()) {
            return List.of();
        }

        // 1. 이름별로 그룹화 (이름이 같은 KOSPI/KOSDAQ 업종 통합, 공백 및 특수문자 제거하여 정규화)
        // NPE 방지를 위해 sectorName 및 indicators가 없는 데이터는 제외
        Map<String, List<SectorInsight>> groupedByName = allInsights.stream()
                .filter(s -> s.getSectorName() != null && s.getIndicators() != null && s.getAvgFluctuationRate() != null && s.getSectorIndexCurrentPrice() != null)
                .collect(Collectors.groupingBy(s -> s.getSectorName().replaceAll("[^가-힣a-zA-Z0-9]", "")));

        // 2. 그룹별 평균 등락률 계산 및 결과 객체 생성
        return groupedByName.entrySet().stream()
                .map(entry -> {
                    List<SectorInsight> group = entry.getValue();
                    String name = group.getFirst().getSectorName(); // 원본 이름 중 하나 사용

                    // 등락률 평균 계산
                    BigDecimal avgRate = group.stream()
                            .map(SectorInsight::getAvgFluctuationRate)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(group.size()), 4, RoundingMode.HALF_UP);

                    // 현재가 평균 (또는 대표값)
                    BigDecimal avgPrice = group.stream()
                            .map(SectorInsight::getSectorIndexCurrentPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(group.size()), 2, RoundingMode.HALF_UP);

                    // 과열 여부 (그룹 내 하나라도 과열이면 과열로 간주)
                    boolean isOverheated = group.stream().anyMatch(SectorInsight::isOverheated);

                    // 대표 코드 (첫 번째 것 사용)
                    String repCode = group.getFirst().getSectorCode();

                    // AI 의견 (첫 번째 것 사용 또는 요약)
                    String diagnosis = generateDiagnosisMessage(group.getFirst());

                    return new SectorRankingResult(repCode, name, avgPrice, avgRate, isOverheated, diagnosis);
                })
                // 3. 평균 등락률 기준 내림차순 정렬
                .sorted((a, b) -> b.fluctuationRate().compareTo(a.fluctuationRate()))
                // 4. 상위 limit(10)개 추출
                .limit(limit)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "sectorDetail", key = "#sectorCode + '_' + #date.toString()")
    public SectorDetailResult getSectorDetail(String sectorCode, LocalDate date) {
        SectorInsight insight = sectorInsightPort.findBySectorCodeAndDate(sectorCode, date)
                .or(() -> sectorInsightPort.findLatestBefore(sectorCode, date))
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        return new SectorDetailResult(
                insight.getSectorCode(), insight.getSectorName(), insight.getBaseDate(),
                insight.getSectorIndexCurrentPrice(), insight.getAvgFluctuationRate(),
                insight.getTechnicalIndicators(), insight.isOverheated(),
                generateDiagnosisMessage(insight), insight.getLeadingStocks(),
                insight.getAiOpinion()
        );
    }

    @Override
    @Cacheable(cacheNames = "sectorComparison", key = "#sectorCode")
    public SectorComparisonResult compareWithMarket(String sectorCode) {
        SectorInsight sector = sectorInsightPort.findLatestBefore(sectorCode, LocalDate.now().plusDays(1))
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        LocalDate effectiveDate = sector.getBaseDate();
        String marketTicker = sector.getMarketType().getBenchmarkTicker();

        // 시장 데이터는 BenchmarkPricePort에서 조회
        var market = benchmarkPricePort.findByTickerAndBaseDate(marketTicker, effectiveDate)
                .orElseThrow(() -> new SectorDomainException(ErrorCode.SECTOR_DATA_NOT_FOUND));

        BigDecimal rs = sector.getAvgFluctuationRate().subtract(market.getChangeRate());
        List<SectorInsight> sectorHistory = sectorInsightPort.findHistoryByCode(sectorCode, effectiveDate, 30);
        List<BenchmarkPrice> marketHistory = benchmarkPricePort.findHistoryByTicker(marketTicker, effectiveDate, 30);
        
        List<SectorComparisonResult.HistoricalRS> history = calculateHistoricalRSFromBenchmark(sectorHistory, marketHistory);

        return new SectorComparisonResult(
                sectorCode, sector.getSectorName(), effectiveDate,
                sector.getAvgFluctuationRate(), market.getChangeRate(),
                rs, resolvePerformanceStatus(rs), history
        );
    }

    private List<SectorComparisonResult.HistoricalRS> calculateHistoricalRSFromBenchmark(
            List<SectorInsight> sectorHistory, 
            List<BenchmarkPrice> marketHistory
    ) {
        Map<LocalDate, BigDecimal> marketMap = marketHistory.stream()
                .collect(Collectors.toMap(
                        BenchmarkPrice::getBaseDate, 
                        BenchmarkPrice::getChangeRate,
                        (v1, v2) -> v1
                ));

        return sectorHistory.stream()
                .filter(s -> marketMap.containsKey(s.getBaseDate()))
                .map(s -> {
                    BigDecimal mRate = marketMap.get(s.getBaseDate());
                    BigDecimal rs = s.getAvgFluctuationRate().subtract(mRate);
                    return new SectorComparisonResult.HistoricalRS(s.getBaseDate(), s.getAvgFluctuationRate(), mRate, rs);
                })
                .sorted(Comparator.comparing(SectorComparisonResult.HistoricalRS::date))
                .toList();
    }

    private String resolvePerformanceStatus(BigDecimal rs) {
        if (rs == null) return "NEUTRAL";
        // 0.4%p 차이를 기준으로 판별 (노이즈 감소를 위해 0.3%에서 추가 상향)
        if (rs.compareTo(new BigDecimal("0.4")) > 0) return "OUTPERFORM";
        if (rs.compareTo(new BigDecimal("-0.4")) < 0) return "UNDERPERFORM";
        return "NEUTRAL";
    }

    private String generateDiagnosisMessage(SectorInsight insight) {
        TechnicalIndicators indicators = insight.getTechnicalIndicators();
        if (indicators == null || indicators.getRsi14() == null) return DiagnosisStatus.DATA_INSUFFICIENT.getMessage();

        boolean rsiOver = indicators.getRsi14().compareTo(new BigDecimal("70")) > 0;
        boolean rsiUnder = indicators.getRsi14().compareTo(new BigDecimal("30")) < 0;
        
        BigDecimal disparity = BigDecimal.ZERO;
        if (indicators.getMa20() != null && indicators.getMa20().compareTo(BigDecimal.ZERO) > 0) {
            disparity = insight.getSectorIndexCurrentPrice().divide(indicators.getMa20(), 4, RoundingMode.HALF_UP);
        }
        boolean disparityOver = disparity.compareTo(new BigDecimal("1.1")) >= 0;

        if (rsiOver && disparityOver) return DiagnosisStatus.OVERHEATED_BOTH.getMessage();
        if (rsiOver) return DiagnosisStatus.OVERHEATED_RSI.getMessage();
        if (disparityOver) return DiagnosisStatus.OVERHEATED_DISPARITY.getMessage();
        if (rsiUnder) return DiagnosisStatus.STAGNANT.getMessage();
        
        return DiagnosisStatus.NORMAL.getMessage();
    }
}
