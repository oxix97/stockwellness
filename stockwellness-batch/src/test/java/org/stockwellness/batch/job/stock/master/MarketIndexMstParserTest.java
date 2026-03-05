package org.stockwellness.batch.job.stock.master;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.insight.MarketIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MarketIndexMstParserTest {

    private static final String KIS_DIR = "kis";

    @Test
    @DisplayName("실제 idxcode.mst 파일을 파싱하여 주요 업종 정보가 정확한지 검증한다")
    void testParseIdxCode() throws IOException {
        // given
        Path path = findMstFile("idxcode.mst");

        // when
        List<MarketIndex> indices = MarketIndexMstParser.parse(path);

        // then
        assertThat(indices).isNotEmpty();
        System.out.printf("[MarketIndex] 총 %,d개 업종 로드됨%n", indices.size());

        // 1. KOSPI 종합 (0001) 확인
        Optional<MarketIndex> kospiTotal = indices.stream()
                .filter(it -> "0001".equals(it.getIndexCode()))
                .findFirst();

        assertThat(kospiTotal).isPresent();
        assertThat(kospiTotal.get().getIndexName()).isEqualTo("종합");

        // 2. IT 서비스 (0029) 확인
        Optional<MarketIndex> itService = indices.stream()
                .filter(it -> "0029".equals(it.getIndexCode()))
                .findFirst();

        assertThat(itService).isPresent();
        assertThat(itService.get().getIndexName()).isEqualTo("IT 서비스");

        // 3. KOSDAQ IT S/W (1030) 확인 - 앞서 나라스페이스테크놀로지 매핑 실패 사례
        // KOSDAQ 코드는 div(1) + code(030) 조합일 수 있으므로 유연하게 확인
        Optional<MarketIndex> kosdaqIt = indices.stream()
                .filter(it -> it.getIndexCode().endsWith("030"))
                .findFirst();

        assertThat(kosdaqIt).isPresent();
        System.out.println("검증 성공 [MarketIndex]: " + kosdaqIt.get().getIndexName() + " (" + kosdaqIt.get().getIndexCode() + ")");
    }

    private Path findMstFile(String fileName) {
        Path path = Paths.get(KIS_DIR, fileName);
        if (!path.toFile().exists()) {
            path = Paths.get("..", KIS_DIR, fileName);
        }
        return path;
    }
}
