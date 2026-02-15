package org.stockwellness.application.port.out.stock;

import java.time.Duration;
import java.util.List;

public interface SearchHistoryPort {
    void save(Long memberId, String keyword);
    List<String> findAll(Long memberId);
    void delete(Long memberId, String keyword);
    void deleteAll(Long memberId);
    void setExpireTime(Long memberId, Duration duration);
}
