package org.stockwellness.domain.stock.analysis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiagnosisStatus {
    OVERHEATED_RSI("현재 섹터는 과열 구간에 진입했습니다. (사유: RSI 지표 과매수) 과매수(Overbought) - 조정 가능성 높음"),
    OVERHEATED_DISPARITY("현재 섹터는 과열 구간에 진입했습니다. (사유: 20일선 이격도 과다) 과매수(Overbought) - 조정 가능성 높음"),
    OVERHEATED_BOTH("현재 섹터는 과열 구간에 진입했습니다. (사유: RSI 및 이격도 복합 과열) 과매수(Overbought) - 조정 가능성 높음"),
    STAGNANT("현재 섹터는 침체 구간에 있습니다. 과매도(Oversold) - 반등 가능성 존재"),
    NORMAL("현재 섹터의 지수 흐름은 정상 범위 내에 있습니다. 중립(Neutral)"),
    DATA_INSUFFICIENT("데이터 부족으로 진단이 불가능합니다.");

    private final String message;
}
