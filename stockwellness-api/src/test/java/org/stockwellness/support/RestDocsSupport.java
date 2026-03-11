package org.stockwellness.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.stockwellness.adapter.out.external.ai.OpenAiAdapter;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;

import static org.mockito.Mockito.withSettings;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
@Import(RestDocsSupport.AiMockConfig.class)
public abstract class RestDocsSupport {

    @TestConfiguration
    public static class AiMockConfig {
        @Bean(name = "openAiAdapter")
        @Primary
        public Object openAiAdapter() {
            return Mockito.mock(OpenAiAdapter.class, withSettings().extraInterfaces(
                    LoadPortfolioAiPort.class,
                    AiAdviceProviderPort.class,
                    LoadSectorAiPort.class
            ));
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint())
                )
                .build();
    }
}
