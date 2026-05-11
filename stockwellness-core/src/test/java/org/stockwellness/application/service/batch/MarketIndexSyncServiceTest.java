package org.stockwellness.application.service.batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.persistence.stock.repository.MarketIndexRepository;
import org.stockwellness.application.parser.MarketIndexMstParser;
import org.stockwellness.domain.stock.insight.MarketIndex;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketIndexSyncService 단위 테스트")
class MarketIndexSyncServiceTest {

    @InjectMocks
    private MarketIndexSyncService marketIndexSyncService;

    @Mock
    private MarketIndexRepository marketIndexRepository;

    private MockedStatic<MarketIndexMstParser> parserMockedStatic;

    @BeforeEach
    void setUp() {
        parserMockedStatic = mockStatic(MarketIndexMstParser.class);
    }

    @AfterEach
    void tearDown() {
        parserMockedStatic.close();
    }

    @Nested
    @DisplayName("지수 동기화(syncIndices) 테스트")
    class SyncIndicesCases {

        @Test
        @DisplayName("파일이 존재하지 않으면 IllegalArgumentException이 발생한다")
        void shouldThrowExceptionWhenFileDoesNotExist() {
            String fileName = "non_existent.mst";
            
            assertThatThrownBy(() -> marketIndexSyncService.syncIndices(fileName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("업종 마스터 파일을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("정상적인 파일이면 파싱된 데이터를 저장한다")
        void shouldSaveParsedIndices() throws IOException {
            // given
            String fileName = "idxcode.mst";
            Path path = Paths.get("kis", fileName);
            
            // 실제 파일이 없으면 IllegalArgumentException이 발생하므로 임시로 생성하거나 Mockito가 Path를 타게 해야 함
            // 하지만 Service 내부에서 Paths.get("kis", fileName)을 호출하고 path.toFile().exists()를 체크함.
            // 테스트 환경에서 "kis" 디렉토리가 없을 수 있으므로, 실제 파일을 생성하는 대신 로직을 검증하려면 
            // 파일 체크 부분을 우회하거나 테스트용 파일을 만들어야 함.
            
            // 임시 파일 생성
            Path kisDir = Paths.get("kis");
            if (!Files.exists(kisDir)) {
                Files.createDirectory(kisDir);
            }
            Path tempFile = kisDir.resolve(fileName);
            Files.createFile(tempFile);

            try {
                MarketIndex index1 = MarketIndex.of("0001", "종합");
                MarketIndex index2 = MarketIndex.of("0002", "대형주");
                
                parserMockedStatic.when(() -> MarketIndexMstParser.parse(any())).thenReturn(List.of(index1, index2));
                given(marketIndexRepository.findAll()).willReturn(List.of());

                // when
                marketIndexSyncService.syncIndices(fileName);

                // then
                verify(marketIndexRepository, times(1)).save(index1);
                verify(marketIndexRepository, times(1)).save(index2);
            } finally {
                Files.deleteIfExists(tempFile);
                // kis 디렉토리는 다른 테스트에 영향을 줄 수 있으므로 일단 둠 (또는 삭제)
            }
        }

        @Test
        @DisplayName("기존에 존재하는 지수는 저장하지 않는다")
        void shouldNotSaveExistingIndices() throws IOException {
            // given
            String fileName = "idxcode.mst";
            Path kisDir = Paths.get("kis");
            if (!Files.exists(kisDir)) Files.createDirectory(kisDir);
            Path tempFile = kisDir.resolve(fileName);
            Files.createFile(tempFile);

            try {
                MarketIndex existing = MarketIndex.of("0001", "종합");
                MarketIndex newIndex = MarketIndex.of("0002", "대형주");
                
                parserMockedStatic.when(() -> MarketIndexMstParser.parse(any())).thenReturn(List.of(existing, newIndex));
                given(marketIndexRepository.findAll()).willReturn(List.of(existing));

                // when
                marketIndexSyncService.syncIndices(fileName);

                // then
                verify(marketIndexRepository, never()).save(existing);
                verify(marketIndexRepository, times(1)).save(newIndex);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
