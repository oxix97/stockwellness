package org.stockwellness.application.port.in.stock;

import java.util.List;

public interface SearchHistoryUseCase {
    /**
     * 검색어 저장 및 TTL 갱신
     */
    void saveSearchHistory(Long memberId, String keyword);

    /**
     * 최근 검색어 목록 조회
     */
    List<String> getRecentSearches(Long memberId);

    /**
     * 특정 검색어 삭제
     */
    void removeSearchHistory(Long memberId, String keyword);

    /**
     * 전체 검색어 삭제
     */
    void clearSearchHistory(Long memberId);
}
