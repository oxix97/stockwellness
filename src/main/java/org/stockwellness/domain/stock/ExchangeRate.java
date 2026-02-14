package org.stockwellness.domain.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "exchange_rate",
    uniqueConstraints = @UniqueConstraint(columnNames = {"currency_pair", "base_date"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 7)
    private String currencyPair; // "USD/KRW"

    @Column(nullable = false)
    private LocalDate baseDate;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal rate;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 생성자, 팩토리 메서드 등
}