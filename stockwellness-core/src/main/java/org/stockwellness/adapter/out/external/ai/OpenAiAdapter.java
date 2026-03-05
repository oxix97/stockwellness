package org.stockwellness.adapter.out.external.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;

import java.util.function.Supplier;

@Slf4j
@Component
public class OpenAiAdapter implements LlmClientPort, LoadPortfolioAiPort, LoadSectorAiPort {

    private final ChatClient chatClient;
    private final PromptTemplateMapper promptTemplateMapper;

    public OpenAiAdapter(ChatClient.Builder builder, PromptTemplateMapper promptTemplateMapper) {
        this.chatClient = builder.build();
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public AiReport generateSectorOpinion(SectorAiContext context) {
        String systemInstruction = promptTemplateMapper.getSectorSystemInstruction();
        String userContext = promptTemplateMapper.toSectorPromptString(context);

        return executeAiCall(
                systemInstruction,
                userContext,
                AiReport.class,
                AiReport::fallback,
                "Sector AI Analysis"
        );
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public PortfolioAiResult generatePortfolioInsight(PortfolioAiContext context) {
        String systemInstruction = promptTemplateMapper.getPortfolioSystemInstruction();
        String userContext = promptTemplateMapper.toPortfolioPromptString(context);

        return executeAiCall(
                systemInstruction,
                userContext,
                PortfolioAiResult.class,
                PortfolioAiResult::fallback,
                "Portfolio AI Diagnosis"
        );
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public AiReport generateInsight(String systemInstruction, AiAnalysisContext context) {
        String userContext = promptTemplateMapper.toPromptString(context);

        return executeAiCall(
                systemInstruction,
                userContext,
                AiReport.class,
                AiReport::fallback,
                "Stock AI Analysis"
        );
    }

    /**
     * 공통 AI 호출 로직 (Generic)
     */
    private <T> T executeAiCall(
            String system,
            String user,
            Class<T> clazz,
            Supplier<T> fallback,
            String taskName
    ) {
        log.info("📡 Requesting {}... (Prompt Length: {})", taskName, user.length());
        var outputConverter = new BeanOutputConverter<>(clazz);

        try {
            return chatClient.prompt()
                    .system(system)
                    .user(u -> u.text(user + "\n\n반드시 아래 JSON 포맷을 준수하여 응답해:\n{format}")
                            .param("format", outputConverter.getFormat()))
                    .call()
                    .entity(outputConverter);
        } catch (Exception e) {
            log.error("❌ {} Failed: {}", taskName, e.getMessage());
            return fallback.get();
        }
    }
}
