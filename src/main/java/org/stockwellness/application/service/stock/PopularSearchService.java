package org.stockwellness.application.service.stock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.in.stock.PopularSearchUseCase;
import org.stockwellness.application.port.out.stock.PopularSearchPort;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularSearchService implements PopularSearchUseCase {

    private final PopularSearchPort popularSearchPort;

    @Override
    public List<String> getPopularSearches() {
        return popularSearchPort.findTop10();
    }
}
