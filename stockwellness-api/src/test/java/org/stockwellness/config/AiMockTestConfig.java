package org.stockwellness.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.stockwellness.adapter.out.external.ai.OpenAiAdapter;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;

import static org.mockito.Mockito.withSettings;

@TestConfiguration
public class AiMockTestConfig {

    @Bean(name = "openAiAdapter")
    @Primary
    public Object openAiAdapter() {
        // 반환 타입을 Object로 선언하여 Spring이 런타임 객체의 인터페이스들을 자유롭게 인식하도록 합니다.
        return Mockito.mock(OpenAiAdapter.class, withSettings().extraInterfaces(
                LoadPortfolioAiPort.class,
                AiAdviceProviderPort.class,
                LoadSectorAiPort.class
        ));
    }
}
