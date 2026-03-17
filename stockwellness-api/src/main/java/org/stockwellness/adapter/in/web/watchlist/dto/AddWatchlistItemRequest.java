package org.stockwellness.adapter.in.web.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddWatchlistItemRequest(
        @NotBlank(message = "티커는 필수입니다.")
        String ticker,

        @Size(max = 200, message = "메모는 최대 200자까지 입력할 수 있습니다.")
        String note
) {
}
