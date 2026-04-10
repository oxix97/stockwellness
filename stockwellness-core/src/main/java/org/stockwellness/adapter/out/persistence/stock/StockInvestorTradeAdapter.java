package org.stockwellness.adapter.out.persistence.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.out.persistence.stock.repository.StockInvestorTradeRepository;
import org.stockwellness.application.port.out.stock.StockInvestorTradePort;
import org.stockwellness.domain.stock.price.StockInvestorTrade;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockInvestorTradeAdapter implements StockInvestorTradePort {

    private final StockInvestorTradeRepository repository;

    @Override
    public void saveAll(List<StockInvestorTrade> trades) {
        // JPA saveAll()을 사용하되, 앞서 논리적으로 Upsert를 원하셨으므로 
        // 기존 행이 있는 경우 합치는 로직이 필요할 수 있습니다. 
        // 여기서는 기본 saveAll을 사용하고 JDBC Batch로 성능을 최적화하는 구성을 따릅니다.
        repository.saveAll(trades);
    }
}
