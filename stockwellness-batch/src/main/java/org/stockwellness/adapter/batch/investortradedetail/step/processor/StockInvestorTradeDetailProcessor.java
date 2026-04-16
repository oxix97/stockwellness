package org.stockwellness.adapter.batch.investortradedetail.step.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.stockwellness.adapter.out.external.kis.dto.InvestorTradeDetail;
import org.stockwellness.adapter.out.persistence.stock.repository.StockRepository;
import org.stockwellness.adapter.batch.investortradedetail.model.InvestorTradeDetailUpdateCommand;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
public class StockInvestorTradeDetailProcessor implements ItemProcessor<InvestorTradeDetail, InvestorTradeDetailUpdateCommand> {

    private final StockRepository stockRepository;
    private final LocalDate marketBaseDate;

    public StockInvestorTradeDetailProcessor(StockRepository stockRepository, LocalDate marketBaseDate) {
        this.stockRepository = stockRepository;
        this.marketBaseDate = marketBaseDate;
    }

    @Override
    public InvestorTradeDetailUpdateCommand process(InvestorTradeDetail item) {
        if (item == null || item.mkscShrnIscd() == null || item.mkscShrnIscd().isBlank()) {
            return null;
        }

        Stock stock = stockRepository.findByTicker(item.mkscShrnIscd()).orElse(null);
        if (stock == null) {
            log.warn("[투자자 상세 배치] 종목 마스터에 없는 ticker라 저장을 건너뜁니다. ticker={}", item.mkscShrnIscd());
            return null;
        }

        return new InvestorTradeDetailUpdateCommand(
                stock.getId(),
                marketBaseDate,
                item.htsKorIsnm(),
                item.mkscShrnIscd(),
                parseLong(item.frgnNtbyQty()),
                parseLong(item.orgnNtbyQty()),
                parseLong(item.ivtrNtbyQty()),
                parseLong(item.bankNtbyQty()),
                parseLong(item.insuNtbyQty()),
                parseLong(item.mrbnNtbyQty()),
                parseLong(item.fundNtbyQty()),
                parseLong(item.etcOrgtNtbyVol()),
                parseLong(item.etcCorpNtbyVol()),
                parseBigDecimal(item.frgnNtbyTrPbmn()),
                parseBigDecimal(item.orgnNtbyTrPbmn()),
                parseBigDecimal(item.ivtrNtbyTrPbmn()),
                parseBigDecimal(item.bankNtbyTrPbmn()),
                parseBigDecimal(item.insuNtbyTrPbmn()),
                parseBigDecimal(item.mrbnNtbyTrPbmn()),
                parseBigDecimal(item.fundNtbyTrPbmn()),
                parseBigDecimal(item.etcOrgtNtbyTrPbmn()),
                parseBigDecimal(item.etcCorpNtbyTrPbmn())
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value.replace(",", "").trim());
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(",", "").trim());
    }
}
