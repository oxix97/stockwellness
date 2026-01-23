package org.stockwellness.adapter.out.external.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.application.port.out.stock.LlmClientPort;

import java.util.List;

@Slf4j
@Component
public class OpenAiAdapter implements LlmClientPort {

    private final ChatClient chatClient;
    private final PromptTemplateMapper promptTemplateMapper;

    public OpenAiAdapter(ChatClient.Builder builder, PromptTemplateMapper promptTemplateMapper) {
        this.chatClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .withModel("gpt-4o-mini") // 기본 모델 설정 (비용 절감)
                        .withTemperature(0.3f)     // 낮을수록 사실적/분석적 (0.0 ~ 1.0)
                        .build())
                .build();
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public AiReport generateInsight(String systemInstruction, AiAnalysisContext context) {
        // 도메인 객체를 프롬프트 문자열로 변환 (Adapter의 역할)
        String userContext = promptTemplateMapper.toPromptString(context);

        log.info("📡 Requesting AI Analysis... (Length: {})", userContext.length());
        var outputConverter = new BeanOutputConverter<>(AiReport.class);

        try {
            // Fluent API를 사용한 호출
            return chatClient.prompt()
                    .system(systemInstruction) // 시스템 프롬프트 (페르소나)
                    .user(u -> u.text(userContext + "\n\n반드시 아래 JSON 포맷을 준수하여 응답해:\n{format}")
                            .param("format", outputConverter.getFormat()))
                    .call()
                    .entity(outputConverter);
        } catch (Exception e) {
            log.error("❌ AI Parsing Failed: {}", e.getMessage());
            // 실패 시 Fallback(기본값) 리턴 또는 에러 전파
            return new AiReport(
                    AiReport.InvestmentDecision.HOLD,
                    0,
                    "분석 시스템 일시 장애",
                    List.of("AI 응답을 처리하는 중 오류가 발생했습니다."),
                    ""
            );
        }
    }
}
