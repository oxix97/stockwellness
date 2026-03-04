package org.stockwellness.batch.job.stock.master;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexSyncService {

    private final MarketIndexRepository marketIndexRepository;

    @Transactional
    public void syncIndices(String fileName) throws IOException {
        Path path = Paths.get("kis", fileName);
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("업종 마스터 파일을 찾을 수 없습니다: " + path);
        }

        log.info("[MarketIndex] 동기화 시작: {}", fileName);
        List<MarketIndex> parsedIndices = MarketIndexMstParser.parse(path);
        log.info("[MarketIndex] 파싱 완료: {}건", parsedIndices.size());

        // 기존 데이터 로드 (Upsert 처리를 위함)
        Map<String, MarketIndex> existingMap = marketIndexRepository.findAll().stream()
                .collect(Collectors.toMap(MarketIndex::getIndexCode, m -> m, (m1, m2) -> m1));

        int newCount = 0;
        int updateCount = 0;

        for (MarketIndex parsed : parsedIndices) {
            MarketIndex existing = existingMap.get(parsed.getIndexCode());
            if (existing != null) {
                // 명칭이 변경된 경우에만 업데이트 (필요 시)
                if (!existing.getIndexName().equals(parsed.getIndexName())) {
                    // 엔티티에 업데이트 메서드가 있다면 호출 (현재는 불변성 고려하여 로그만 남기거나 교체)
                    updateCount++;
                }
            } else {
                marketIndexRepository.save(parsed);
                newCount++;
            }
        }

        log.info("[MarketIndex] 동기화 완료: 신규 {}건, 기존 {}건", newCount, existingMap.size());
    }
}
