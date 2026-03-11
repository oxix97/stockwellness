package org.stockwellness.application.port.in.portfolio.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.portfolio.advisor.AdviceAction;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdviceResponse DTO 단위 테스트")
class AdviceResponseTest {

    @Test
    @DisplayName("AdviceResponse를 생성할 수 있다")
    void create_advice_response_success() {
        // given
        String content = "리밸런싱 조언";
        AdviceAction action = AdviceAction.REBALANCE;
        LocalDateTime createdAt = LocalDateTime.now();

        // when
        AdviceResponse response = new AdviceResponse(content, action, createdAt);

        // then
        assertThat(response.content()).isEqualTo(content);
        assertThat(response.action()).isEqualTo(action);
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
