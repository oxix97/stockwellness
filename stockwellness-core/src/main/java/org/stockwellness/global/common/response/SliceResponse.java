package org.stockwellness.global.common.response;

import org.springframework.data.domain.Slice;

import java.util.List;

public record SliceResponse<T>(
        List<T> content,
        int number,
        int size,
        int numberOfElements,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean empty
) {
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.getNumberOfElements(),
                slice.isFirst(),
                slice.isLast(),
                slice.hasNext(),
                slice.isEmpty()
        );
    }
}
