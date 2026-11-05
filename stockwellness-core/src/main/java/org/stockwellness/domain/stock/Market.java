package org.stockwellness.domain.stock;

import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.stockwellness.global.error.exception.GlobalException;

import static org.stockwellness.global.error.ErrorCode.INVALID_INPUT_VALUE;

@Entity
@Table(name = "market")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Market {
    @Id
    @Column(length = 20, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", nullable = false, length = 2, columnDefinition = "char(2)")
    private String countryCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(nullable = false, length = 50)
    private String timezone;

    public static Market of(String code, String name, String countryCode, String currency, String timezone) {
        if (code == null || !code.matches("[A-Z0-9_-]{1,20}") || !text(name, 100)
                || countryCode == null || !countryCode.matches("[A-Z]{2}")
                || currency == null || !currency.matches("[A-Z]{3}")
                || timezone == null || timezone.length() > 50 || !validZone(timezone)) {
            throw new GlobalException(INVALID_INPUT_VALUE);
        }
        Market market = new Market();
        market.code = code;
        market.name = name;
        market.countryCode = countryCode;
        market.currency = currency;
        market.timezone = timezone;
        return market;
    }

    private static boolean text(String value, int maxLength) {
        return value != null && !value.isBlank() && value.length() <= maxLength;
    }

    private static boolean validZone(String timezone) {
        try {
            return ZoneId.of(timezone).getId().equals(timezone);
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
