package org.stockwellness.adapter.out.persistence.stock.repository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.stockwellness.domain.stock.StockHistory;

import java.sql.Date;
import java.util.List;

/**
 * 대용량 데이터 처리를 위한 JDBC 전용 Repository 인터페이스
 * <p>JPA의 오버헤드 없이 DB에 직접 Bulk Insert/Update를 수행합니다.</p>
 */
@Repository
@RequiredArgsConstructor
public class StockHistoryJdbcRepository {

    private final JdbcTemplate jdbcTemplate;


}