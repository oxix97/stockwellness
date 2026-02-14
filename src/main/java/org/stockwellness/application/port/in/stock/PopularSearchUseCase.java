package org.stockwellness.application.port.in.stock;

import java.util.List;

public interface PopularSearchUseCase {
    /**
     * 현재 인기 검색어 Top 10 조회
     */
    List<String> getPopularSearches();
}
