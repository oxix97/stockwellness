package org.stockwellness.batch.job.investortradedetail.step.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.batch.job.investortradedetail.model.InvestorTradeDetailUpdateSource;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class StockInvestorTradeDetailProcessor implements ItemProcessor<InvestorTradeDetailUpdateSource, InvestorTradeDetailUpdateCommand> {

    private static final BigDecimal PBMN_MULTIPLIER = BigDecimal.valueOf(1_000_000L);

    private final StockRepository stockRepository;
    private final LocalDate baseDate;
    private final Map<String, Stock> stockCache = new HashMap<>();

    public StockInvestorTradeDetailProcessor(StockRepository stockRepository, LocalDate baseDate) {
        this.stockRepository = stockRepository;
        this.baseDate = baseDate;
    }

    @Override
    public InvestorTradeDetailUpdateCommand process(InvestorTradeDetailUpdateSource item) {
        String ticker = normalizeTicker(item.ticker());
        if (!StringUtils.hasText(ticker)) {
            return null;
        }

        Stock stock = stockCache.get(ticker);
        if (!stockCache.containsKey(ticker)) {
            stock = stockRepository.findByTicker(ticker).orElse(null);
            stockCache.put(ticker, stock);
        }
        if (stock == null) {
            log.warn("[투자주체 보정 Processor] 종목 매핑을 찾지 못해 건너뜁니다. ticker={}", ticker);
            return null;
        }

        BigDecimal institutionalAmount = parsePbmn(item.institutionalBuyingAmtText(), ticker, "institutional");
        BigDecimal foreignAmount = parsePbmn(item.foreignBuyingAmtText(), ticker, "foreign");
        if (institutionalAmount == null || foreignAmount == null) {
            return null;
        }

        return new InvestorTradeDetailUpdateCommand(
                stock.getId(),
                baseDate,
                institutionalAmount,
                foreignAmount
        );
    }

    private BigDecimal parsePbmn(String raw, String ticker, String fieldName) {
        if (!StringUtils.hasText(raw)) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(raw.replace(",", "").trim()).multiply(PBMN_MULTIPLIER);
        } catch (NumberFormatException exception) {
            log.warn(
                    "[투자주체 보정 Processor] 금액 파싱에 실패해 건너뜁니다. ticker={}, field={}, value={}",
                    ticker,
                    fieldName,
                    raw
            );
            return null;
        }
    }

    private String normalizeTicker(String ticker) {
        return ticker == null ? null : ticker.trim().toUpperCase(Locale.ROOT);
    }
}
