package org.stockwellness.adapter.out.external.krx.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * KRX 공공데이터 API 공통 응답 Wrapper
 * @param <T> 실제 Item 데이터의 타입 (예: KrxStockItem)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KrxCommonResponse<T>(
        Response<T> response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response<T>(
            Header header,
            Body<T> body
    ) {
    }

    public record Header(
            String resultCode,
            String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body<T>(
            int numOfRows,
            int pageNo,
            int totalCount,
            Items<T> items
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items<T>(
            @JsonProperty("item")
            List<T> itemList
    ) {
    }

    /**
     * 편의 메서드: Null Safety를 고려하여 아이템 리스트를 반환
     */
    public List<T> getItems() {
        if (response == null || response.body() == null || 
            response.body().items() == null || response.body().items().itemList() == null) {
            return Collections.emptyList();
        }
        return response.body().items().itemList();
    }
}