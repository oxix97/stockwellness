package org.stockwellness.config;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.stockwellness.application.port.in.stock.result.StockAnalysisResult;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.domain.stock.analysis.InvestmentDecision;
import org.stockwellness.domain.stock.analysis.TrendStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Redis Record 직렬화 예외 재현 테스트")
class RedisRecordSerializationTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Record 객체(StockAnalysisResult)를 캐싱하고 조회할 때 정상 작동하는지 확인")
    void record_serialization_test() {
        // Given
        Cache aiCache = cacheManager.getCache("ai_analysis");
        assertThat(aiCache).isNotNull();

        String isinCode = "005930";
        AiReport report = new AiReport(
                InvestmentDecision.BUY,
                80,
                "테스트 제목",
                List.of("이유1", "이유2", "이유3"),
                "상세 분석 내용"
        );
        StockAnalysisResult result = new StockAnalysisResult(isinCode, TrendStatus.REGULAR, report);

        // When & Then
        assertThatCode(() -> {
            aiCache.put(isinCode, result);
            StockAnalysisResult cachedResult = aiCache.get(isinCode, StockAnalysisResult.class);
            assertThat(cachedResult).isNotNull();
            assertThat(cachedResult.isinCode()).isEqualTo(isinCode);
        }).doesNotThrowAnyException();
    }
}
