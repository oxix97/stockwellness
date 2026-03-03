package org.stockwellness.global.util;

/**
 * SQL 쿼리 작성 시 PostgreSQL의 엄격한 타입 체크를 해결하기 위한 유틸리티 클래스입니다.
 * 가상 테이블(VALUES) 사용 시 데이터베이스가 파라미터 타입을 정확히 추론할 수 있도록 명시적 캐스팅 구문을 제공합니다.
 */
public class QueryTypeUtil {

    // 정수형 (Long, Integer 등)
    public static final String BIGINT = "CAST(? AS bigint)";
    
    // 날짜형 (LocalDate 등)
    public static final String DATE = "CAST(? AS date)";
    
    // 숫자형 (BigDecimal 등)
    public static final String NUMERIC = "CAST(? AS numeric)";

    private QueryTypeUtil() {
        // 인스턴스화 방지
    }
}
