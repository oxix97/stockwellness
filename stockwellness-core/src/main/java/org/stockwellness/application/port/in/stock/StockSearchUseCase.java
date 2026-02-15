package org.stockwellness.application.port.in.stock;

import java.util.List;

/**
 * 종목 검색 히스토리 및 인기 검색어 관리를 담당하는 통합 유스케이스
 */
public interface StockSearchUseCase {

    /**
     * 검색 히스토리를 저장합니다.
     */
    void saveSearchHistory(Long memberId, String keyword);

    /**
     * 최근 검색어 목록을 조회합니다.
     */
    List<String> getRecentSearches(Long memberId);

    /**
     * 특정 검색 히스토리를 삭제합니다.
     */
    void removeSearchHistory(Long memberId, String keyword);

    /**
     * 모든 검색 히스토리를 삭제합니다.
     */
    void clearSearchHistory(Long memberId);

    /**
     * 인기 검색어 Top 10 목록을 조회합니다.
     */
    List<String> getPopularSearches();
}
