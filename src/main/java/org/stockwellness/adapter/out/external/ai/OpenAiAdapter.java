package org.stockwellness.adapter.out.external.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;
import org.stockwellness.application.port.out.stock.LlmClientPort;

import java.util.List;

@Slf4j
@Component
public class OpenAiAdapter implements LlmClientPort, LoadPortfolioAiPort {

    private final ChatClient chatClient;
    private final PromptTemplateMapper promptTemplateMapper;

        private static final String PORTFOLIO_SYSTEM_INSTRUCTION = """

                당신은 "성장하는 궁수(Growth Archer)" 스타일의 투자 전문가입니다. 

                초보 투자자들에게 친근하고 이해하기 쉬운 언어로 포트폴리오 상태를 진단해  줍니다.

                

                [응답 지침]

                1. summary: 포트폴리오의 특징을 나타내는 짧은 별명 (예: "안정적인 방패", "공격적인 불화살")

                2. insight: 현재 포트폴리오의 강점과 약점을 분석한 총평 (2~3문장)

                3. nextSteps: 포트폴리오 개선을 위한 구체적인 실행 단계 (리스트 형태, 최소 2개)

                

                반드시 초보자의 눈높이에 맞춰 친절하게 설명하세요.

                """;

    

        private static final String ERROR_FALLBACK_SUMMARY = "진단 일시 장애";

        private static final String ERROR_FALLBACK_INSIGHT = "AI 인사이트를 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도 해주세요.";

        private static final List<String> ERROR_FALLBACK_STEPS = List.of("데이터를 다시 확인해주세요.", "시스템 관리자에게 문의하세요.");

    

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
    public PortfolioAiResult generatePortfolioInsight(PortfolioAiContext context) {
        String userContext = promptTemplateMapper.toPortfolioPromptString(context);

        log.info("📡 Requesting Portfolio AI Diagnosis... (Length: {})", userContext.length());
        var outputConverter = new BeanOutputConverter<>(PortfolioAiResult.class);

        try {
            return chatClient.prompt()
                    .system(PORTFOLIO_SYSTEM_INSTRUCTION)
                    .user(u -> u.text(userContext + "\n\n반드시 아래 JSON 포맷을 준수하여 응답해:\n{format}")
                            .param("format", outputConverter.getFormat()))
                                        .call()
                                        .entity(outputConverter);
                            } catch (Exception e) {
                                log.error("❌ Portfolio AI Parsing Failed: {}", e.getMessage());
                                return new PortfolioAiResult(
                                        ERROR_FALLBACK_SUMMARY,
                                        ERROR_FALLBACK_INSIGHT,
                                        ERROR_FALLBACK_STEPS
                                );
                            }
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
