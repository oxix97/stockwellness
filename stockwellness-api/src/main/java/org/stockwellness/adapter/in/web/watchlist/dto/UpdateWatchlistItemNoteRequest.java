package org.stockwellness.adapter.in.web.watchlist.dto;

import jakarta.validation.constraints.Size;

public record UpdateWatchlistItemNoteRequest(
        @Size(max = 200, message = "메모는 최대 200자까지 입력할 수 있습니다.")
        String note
) {
}
