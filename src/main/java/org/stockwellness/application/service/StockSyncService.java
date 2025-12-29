//package org.stockwellness.application.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.stockwellness.adapter.out.external.krx.KrxClient;
//import org.stockwellness.adapter.out.external.krx.dto.KrxListedInfoResponse;
//import org.stockwellness.adapter.out.external.krx.dto.KrxStockPriceResponse;
//import org.stockwellness.adapter.out.persistence.stock.StockHistoryAdapter;
//import org.stockwellness.adapter.out.persistence.stock.StockAdapter;
//import org.stockwellness.application.service.mapper.StockMapper;
//import org.stockwellness.domain.stock.StockHistory;
//import org.stockwellness.adapter.out.persistence.stock.repository.StockHistoryJdbcRepository;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class StockSyncService {
//    private final KrxClient krxClient;
//    private final StockAdapter stockAdapter;
//    private final StockHistoryAdapter stockHistoryAdapter;
//    private final StockHistoryJdbcRepository stockHistoryJdbcRepository;
//    private final StockMapper stockMapper;
//
//    public void syncStockMasterData(String baseDate) {
//        List<KrxListedInfoResponse.Item> items = krxClient.stockInfos(baseDate).response().body().items().itemList();
//
//        items.forEach(it -> {
//            stockAdapter.findByTicker(it.ticker())
//                    .ifPresentOrElse(
//                            exist -> exist.updateInfo(exist.getName(), exist.getTicker(), exist.getMarketType(), exist.getTotalShares(), exist.getCorporationNo(), exist.getCorporationName()),
//                            () -> stockAdapter.save(stockMapper.toStockEntity(it))
//                    );
//        });
//    }
//
//    /**
//     * [Sync] 일별 시세 데이터 대량 등록 (Bulk Insert)
//     * - 성능을 위해 JPA saveAll() 대신 JDBC Batch 사용
//     * - 배치 작업(Spring Batch)에서 Chunk 단위로 호출됨
//     */
//    public void saveDailyHistory(String baseDate) {
//        // 1. DTO -> Entity 변환
//        List<StockHistory> histories = krxClient.stockInfos(baseDate).getItems().stream()
//                .map(stockMapper::toHistoryEntity)
//                .toList();
//
//        // 2. Bulk Upsert 수행 (중복 시 업데이트)
//        // JDBC 레포지토리 내부 구현은 "INSERT ... ON CONFLICT DO UPDATE" 쿼리 사용 권장
//        stockHistoryAdapter.saveAll(histories);
//
//        log.info("Saved {} price history records via Bulk Insert.", histories.size());
//    }
//
//    public KrxStockPriceResponse stockPriceInfo(String name) {
//        return krxClient.stockPriceInfo(name);
//    }
//
//    public void loadYearsData() {
//        LocalDate start = LocalDate.of(2021, 1, 1);
//        LocalDate end = LocalDate.of(2021, 12, 31);
//    }
//
//    private void fetchAndSaveDay(LocalDate date) {
//        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//
//        KrxListedInfoResponse response = krxClient.stockInfos(dateStr);
//
//        if (response == null || response.response().body().totalCount() == 0)
//            return;
//
//        List<StockHistory> histories = response.getItems().stream()
//                .map(stockMapper::toHistoryEntity)
//                .toList();
//
//        stockHistoryJdbcRepository.batchBulkInsert(histories);
//    }
//}
