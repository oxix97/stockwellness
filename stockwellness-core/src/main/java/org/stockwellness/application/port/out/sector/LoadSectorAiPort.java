package org.stockwellness.application.port.out.sector;

import org.stockwellness.domain.stock.analysis.AiReport;

/**
 * 섹터별 AI 분석 의견을 생성하기 위한 포트 인터페이스
 */
public interface LoadSectorAiPort {
    /**
     * 특정 섹터의 분석 컨텍스트를 받아 AI 분석 결과(AiReport)를 반환합니다.
     */
    AiReport generateSectorOpinion(SectorAiContext context);
}
