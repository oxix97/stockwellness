package org.stockwellness.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.stock.LlmClientPort;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    @MockitoBean
    protected LoadPortfolioAiPort loadPortfolioAiPort;

    @MockitoBean
    protected AiAdviceProviderPort aiAdviceProviderPort;

    @MockitoBean
    protected LoadSectorAiPort loadSectorAiPort;

    @MockitoBean
    protected LlmClientPort llmClientPort;

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

    /**
     * ApiResponse의 공통 필드 정의 (data 제외)
     */
    protected List<FieldDescriptor> commonResponseFields() {
        return List.of(
                fieldWithPath("success").description("성공 여부"),
                fieldWithPath("status").description("HTTP 상태 코드"),
                fieldWithPath("code").description("비즈니스 상세 코드"),
                fieldWithPath("message").description("결과 메시지"),
                fieldWithPath("timestamp").description("응답 시간"),
                fieldWithPath("traceId").description("에러 추적용 ID (성공 시 null)").optional(),
                fieldWithPath("errors").description("상세 필드 에러 목록 (성공 시 빈 리스트)").optional()
        );
    }

    /**
     * 응답 데이터가 없는(null) 경우의 공통 필드 정의 (data 포함)
     */
    protected List<FieldDescriptor> commonResponseFieldsWithNoData() {
        List<FieldDescriptor> fields = new ArrayList<>(commonResponseFields());
        fields.add(fieldWithPath("data").description("응답 데이터 (null)").optional());
        return fields;
    }
}
