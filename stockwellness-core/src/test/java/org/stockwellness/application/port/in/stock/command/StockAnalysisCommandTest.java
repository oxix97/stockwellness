package org.stockwellness.application.port.in.stock.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockAnalysisCommandTest {

    @Test
    @DisplayName("ISIN 코드는 공백이 제거되고 대문자로 정규화되어야 한다")
    void normalizeIsinCode() {
        // given
        String input1 = "  005930  ";
        String input2 = "kr7005930003";

        // when
        StockAnalysisCommand command1 = new StockAnalysisCommand(input1);
        StockAnalysisCommand command2 = new StockAnalysisCommand(input2);

        // then
        assertThat(command1.isinCode()).isEqualTo("005930");
        assertThat(command2.isinCode()).isEqualTo("KR7005930003");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("ISIN 코드가 빈 값이나 공백이면 IllegalArgumentException이 발생한다")
    void throwExceptionWhenIsinCodeIsBlank(String blankInput) {
        assertThatThrownBy(() -> new StockAnalysisCommand(blankInput))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ISIN 코드가 null이면 IllegalArgumentException이 발생한다")
    void throwExceptionWhenIsinCodeIsNull() {
        assertThatThrownBy(() -> new StockAnalysisCommand(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
