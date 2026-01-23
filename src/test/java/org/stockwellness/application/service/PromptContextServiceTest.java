//package org.stockwellness.application.service;
//
//
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryRepository;
//import org.stockwellness.domain.stock.StockHistory;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.Collections;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.BDDMockito.given;
//
//@ExtendWith(MockitoExtension.class)
//class PromptContextServiceTest {
//
//    @InjectMocks
//    private PromptContextService promptContextService;
//
//    @Mock
//    private StockHistoryRepository stockHistoryRepository;
//
//    private final String ISIN_CODE = "KR7005930003"; // 삼성전자 예시
//
//    @Test
//    @DisplayName("[성공] 데이터가 충분하고 골든크로스가 발생했을 때, 핵심 키워드가 포함되어야 한다.")
//    void generateContext_GoldenCross() {
//        // given
//        // 어제: 5일선(900) < 20일선(1000) -> 역배열
//        StockHistory yesterday = createStockHistory(
//                LocalDate.of(2023, 10, 1), BigDecimal.valueOf(19000),
//                BigDecimal.valueOf(900), BigDecimal.valueOf(1000), // MA5 < MA20
//                BigDecimal.valueOf(40), BigDecimal.valueOf(-5)
//        );
//
//        // 오늘: 5일선(1100) > 20일선(1000) -> 정배열 (골든크로스 발생!)
//        StockHistory today = createStockHistory(
//                LocalDate.of(2023, 10, 2), BigDecimal.valueOf(20000),
//                BigDecimal.valueOf(1100), BigDecimal.valueOf(1000), // MA5 > MA20
//                BigDecimal.valueOf(75), BigDecimal.valueOf(10) // RSI 75(과매수), MACD 양수
//        );
//
//        given(stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(ISIN_CODE))
//                .willReturn(List.of(today, yesterday));
//
//        // when
//        String result = promptContextService.generatePromptContext(ISIN_CODE);
//
//        // then
//        assertThat(result)
//                .contains("[기술적 지표 정밀 분석 보고서]") // 헤더 확인
//                .contains("골든크로스")            // 이벤트 확인
//                .contains("과매수 구간")            // RSI 해석 확인 (75 >= 70)
//                .contains("상승 추세 구간");          // MACD 해석 확인
//
//        System.out.println("=== Generated Prompt Context (Golden Cross) ===\n" + result);
//    }
//
//    @Test
//    @DisplayName("[성공] 데드크로스 및 과매도 구간이 정확히 감지되어야 한다.")
//    void generateContext_DeadCross_Oversold() {
//        // given
//        // 어제: 정배열
//        StockHistory yesterday = createStockHistory(
//                LocalDate.now().minusDays(1), BigDecimal.valueOf(20000),
//                BigDecimal.valueOf(1100), BigDecimal.valueOf(1000),
//                null, null
//        );
//
//        // 오늘: 역배열 (데드크로스), RSI 20 (과매도)
//        StockHistory today = createStockHistory(
//                LocalDate.now(), BigDecimal.valueOf(19000),
//                BigDecimal.valueOf(900), BigDecimal.valueOf(1000),
//                BigDecimal.valueOf(20), BigDecimal.valueOf(-5)
//        );
//
//        given(stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(ISIN_CODE))
//                .willReturn(List.of(today, yesterday));
//
//        // when
//        String result = promptContextService.generatePromptContext(ISIN_CODE);
//
//        // then
//        assertThat(result)
//                .contains("데드크로스")
//                .contains("과매도 구간") // RSI 20 <= 30
//                .contains("하락 추세 구간");
//    }
//
//    @Test
//    @DisplayName("[실패] 데이터가 아예 없는 경우, 지정된 에러 메시지를 반환해야 한다.")
//    void generateContext_NoData() {
//        // given
//        given(stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(ISIN_CODE))
//                .willReturn(Collections.emptyList());
//
//        // when
//        String result = promptContextService.generatePromptContext(ISIN_CODE);
//
//        // then
//        assertThat(result).isEqualTo(StockAnalysisPromptTemplate.MSG_DATA_INSUFFICIENT);
//    }
//
//    @Test
//    @DisplayName("[엣지케이스] 데이터가 하루치(오늘)만 있는 경우, 전일 비교 로직은 건너뛰고 생성되어야 한다.")
//    void generateContext_OnlyTodayData() {
//        // given
//        StockHistory today = createStockHistory(
//                LocalDate.now(), BigDecimal.valueOf(10000),
//                BigDecimal.valueOf(10000), BigDecimal.valueOf(9000),
//                BigDecimal.valueOf(50), BigDecimal.ZERO
//        );
//
//        given(stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(ISIN_CODE))
//                .willReturn(List.of(today)); // 리스트 사이즈 1
//
//        // when
//        String result = promptContextService.generatePromptContext(ISIN_CODE);
//
//        // then
//        assertThat(result).isNotNull();
//        assertThat(result).contains("전일 데이터 없음"); // 등락폭 계산 부분
//        assertThat(result).doesNotContain("크로스");   // 크로스 비교 불가하므로 없어야 함
//    }
//
//    @Test
//    @DisplayName("[엣지케이스] 지표 값(MA, RSI 등)이 Null이어도 에러 없이 '불명/데이터 부족' 처리되어야 한다.")
//    void generateContext_NullIndicators() {
//        // given
//        StockHistory today = StockHistory.create(
//                ISIN_CODE, LocalDate.now(),
//                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
//                BigDecimal.valueOf(10000), BigDecimal.valueOf(10000),
//                BigDecimal.ZERO, BigDecimal.ZERO, 1000L, BigDecimal.ZERO, BigDecimal.ZERO
//        );
//        // 지표 필드는 update 메서드를 호출하지 않았으므로 모두 Null 상태
//
//        given(stockHistoryRepository.findTop2ByIsinCodeOrderByBaseDateDesc(ISIN_CODE))
//                .willReturn(List.of(today));
//
//        // when
//        String result = promptContextService.generatePromptContext(ISIN_CODE);
//
//        // then
//        assertThat(result).contains("데이터 부족"); // MA 분석 결과
//        assertThat(result).contains("불명");      // 위치 판단 결과
//    }
//
//    // --- Helper Method ---
//    private StockHistory createStockHistory(LocalDate date, BigDecimal close,
//                                            BigDecimal ma5, BigDecimal ma20,
//                                            BigDecimal rsi, BigDecimal macd) {
//        StockHistory history = StockHistory.create(
//                ISIN_CODE, date, close, close, close, close,
//                BigDecimal.ZERO, BigDecimal.ZERO, 100000L, BigDecimal.valueOf(100000000), BigDecimal.valueOf(1000000000)
//        );
//        history.updateMa5(ma5);
//        history.updateMa20(ma20);
//        history.updateRsi14(rsi);
//        history.updateMacd(macd);
//        return history;
//    }
//}
