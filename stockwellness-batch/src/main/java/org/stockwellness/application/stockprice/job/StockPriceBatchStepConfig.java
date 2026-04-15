//package org.stockwellness.service.stockprice.job;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.task.TaskExecutor;
//import org.springframework.dao.RecoverableDataAccessException;
//import org.springframework.dao.TransientDataAccessException;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.stockwellness.adapter.out.external.kis.exception.KisApiException;
//import org.stockwellness.service.stockprice.listener.StockPriceSyncEventListener;
//import org.stockwellness.service.stockprice.step.processor.StockPriceFetchProcessor;
//import org.stockwellness.service.stockprice.step.processor.StockPriceIndicatorProcessor;
//import org.stockwellness.service.stockprice.step.reader.StockListReader;
//import org.stockwellness.service.stockprice.step.writer.StockPriceListWriter;
//import org.stockwellness.batch.support.BatchMdcListener;
//import org.stockwellness.batch.support.listener.BatchFailureItemListener;
//import org.stockwellness.batch.support.logging.CommonBatchProgressLoggingListener;
//import org.stockwellness.batch.support.logging.CommonBatchStepLoggingListener;
//import org.stockwellness.domain.stock.Stock;
//import org.stockwellness.domain.stock.price.StockPrice;
//
//import java.util.List;
//
//@Configuration
//@RequiredArgsConstructor
//public class StockPriceBatchStepConfig {
//
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//    private final BatchMdcListener mdcListener;
//    private final CommonBatchStepLoggingListener commonBatchStepLoggingListener;
//    private final CommonBatchProgressLoggingListener commonBatchProgressLoggingListener;
//    private final StockPriceSyncEventListener eventListener;
//    private final TaskExecutor kisBatchExecutor;
//
//    /**
//     * [Step 2-1] 시세 수집 단계: [KIS] 멀티종목시세 조회를 통해 원시 가격 데이터를 저장
//     */
//    @Bean
//    public Step stockPriceFetchStep(
//            StockListReader stockPriceFetchReader,
//            StockPriceFetchProcessor stockPriceFetchProcessor,
//            StockPriceListWriter stockPriceFetchWriter,
//            BatchFailureItemListener<List<StockPrice>> stockPriceFailureListener
//    ) {
//        return new StepBuilder("stockPriceFetchStep", jobRepository)
//                .<List<Stock>, List<StockPrice>>chunk(1, transactionManager)
//                .reader(stockPriceFetchReader)
//                .processor(stockPriceFetchProcessor)
//                .writer(stockPriceFetchWriter)
//                .taskExecutor(kisBatchExecutor)
//                .listener(mdcListener)
//                .listener(commonBatchStepLoggingListener)
//                .listener(commonBatchProgressLoggingListener)
//                .listener(eventListener)
//                .listener(stockPriceFailureListener)
//                .faultTolerant()
//                .retryLimit(3)
//                .retry(TransientDataAccessException.class)
//                .retry(RecoverableDataAccessException.class)
//                .retry(KisApiException.class)
//                .build();
//    }
//
//    /**
//     * [Step 2-2] 지표 계산 단계: 수집된 시세를 바탕으로 기술적 지표 일괄 계산
//     */
//    @Bean
//    public Step stockPriceIndicatorStep(
//            @Qualifier("stockIndicatorReader") StockListReader stockIndicatorReader,
//            StockPriceIndicatorProcessor stockPriceIndicatorProcessor,
//            @Qualifier("stockPriceListWriter") ItemWriter<List<StockPrice>> stockPriceListWriter
//    ) {
//        return new StepBuilder("stockPriceIndicatorStep", jobRepository)
//                .<List<Stock>, List<StockPrice>>chunk(10, transactionManager)
//                .reader(stockIndicatorReader)
//                .processor(stockPriceIndicatorProcessor)
//                .writer(stockPriceListWriter)
//                .listener(mdcListener)
//                .listener(commonBatchStepLoggingListener)
//                .listener(commonBatchProgressLoggingListener)
//                .build();
//    }
//}
