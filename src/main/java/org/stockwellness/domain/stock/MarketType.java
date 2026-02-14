package org.stockwellness.domain.stock;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketType {
    KOSPI("코스피"),
    KOSDAQ("코스닥"),
    NASDAQ("나스닥"),
    NYSE("뉴욕"),
    AMEX("아멕스");

    private final String description;
}