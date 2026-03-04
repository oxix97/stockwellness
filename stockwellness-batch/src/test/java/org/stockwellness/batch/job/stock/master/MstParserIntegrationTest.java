package org.stockwellness.batch.job.stock.master;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.KosdaqItem;
import org.stockwellness.domain.stock.KospiItem;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MstParserIntegrationTest {

    private static final String KIS_DIR = "kis";

    @Test
    @DisplayName("실제 kospi_code.mst 파일을 파싱하여 삼성전자 등 주요 종목 정보가 정확한지 검증한다")
    void testKospiParsing() throws IOException {
        // given
        Path path = findMstFile("kospi_code.mst");

        // when
        List<KospiItem> items = KospiMstParser.parse(path);

        // then
        assertThat(items).isNotEmpty();
        System.out.printf("[KOSPI] 총 %,d개 종목 로드됨%n", items.size());

        // 디버깅: 상위 10개 출력
        items.stream().limit(10).forEach(it -> 
            System.out.printf(" - Code: [%s], Name: [%s], Group: [%s]%n", 
                it.shortCode(), it.koreanName(), it.groupCode())
        );

        // 삼성전자(005930) 검색
        Optional<KospiItem> samsung = items.stream()
                .filter(it -> "005930".equals(it.shortCode()))
                .findFirst();

        assertThat(samsung).isPresent();
        samsung.ifPresent(it -> {
            assertThat(it.koreanName()).contains("삼성전자");
            assertThat(it.groupCode()).isEqualTo("ST");
            assertThat(it.listingDate()).hasSize(8); // YYYYMMDD
            assertThat(it.marketCapAsLong()).isGreaterThan(0L);

            // 업종 코드 검증 (0000이 아니어야 함)
            System.out.println("검증 [KOSPI] 삼성전자 업종 코드:");
            System.out.println(" - Large: " + it.sectorLarge());
            System.out.println(" - Medium: " + it.sectorMedium());
            assertThat(it.sectorMedium()).isNotEqualTo("0000");

            System.out.println("검증 성공 [KOSPI]: " + it.koreanName() + " (" + it.shortCode() + ")");
        });
        }

        @Test
        @DisplayName("실제 kosdaq_code.mst 파일을 파싱하여 주요 종목 및 코스닥 전용 필드를 검증한다")
        void testKosdaqParsing() throws IOException {
        // given
        Path path = findMstFile("kosdaq_code.mst");

        // when
        List<KosdaqItem> items = KosdaqMstParser.parse(path);

        // then
        assertThat(items).isNotEmpty();
        System.out.printf("[KOSDAQ] 총 %,d개 종목 로드됨%n", items.size());

        // 나라스페이스(478340) 또는 알려진 종목 검색
        Optional<KosdaqItem> nara = items.stream()
                .filter(it -> "478340".equals(it.shortCode()))
                .findFirst();

        nara.ifPresent(it -> {
            System.out.println("검증 [KOSDAQ] 나라스페이스 업종 코드:");
            System.out.println(" - Medium: " + it.sectorMedium());
            assertThat(it.sectorMedium()).isEqualTo("1030"); // IT S/W & SVC
        });

        KosdaqItem firstStock = items.stream()
                .filter(KosdaqItem::isStock)
                .findFirst()
                .orElseThrow();

        assertThat(firstStock.shortCode()).isNotEmpty();
        assertThat(firstStock.koreanName()).isNotEmpty();
        assertThat(firstStock.listingDate()).hasSize(8);
        
        // 코스닥 전용 ROE (정수형) 검증
        // ROE 필드가 숫자로만 구성되어 있거나 마이너스 기호를 포함해야 함
        assertThat(firstStock.roe().strip()).matches("-?\\d+");

        System.out.println("검증 성공 [KOSDAQ]: " + firstStock.koreanName() + " (" + firstStock.shortCode() + ")");
        System.out.println(" - 그룹코드: " + firstStock.groupCode());
        System.out.println(" - ROE(정수): " + firstStock.roeAsLong() + "%");
    }

    private Path findMstFile(String fileName) {
        // 프로젝트 루트 기준 또는 모듈 기준 경로 탐색
        Path path = Paths.get(KIS_DIR, fileName);
        if (!path.toFile().exists()) {
            path = Paths.get("..", KIS_DIR, fileName);
        }
        if (!path.toFile().exists()) {
            throw new IllegalStateException("MST 파일을 찾을 수 없습니다: " + fileName);
        }
        return path;
    }
}
