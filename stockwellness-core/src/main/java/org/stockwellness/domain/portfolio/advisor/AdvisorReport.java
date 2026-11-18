package org.stockwellness.domain.portfolio.advisor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "portfolio_ai_report")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvisorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "model_version", nullable = false, length = 100)
    private String modelVersion;

    @Column(name = "prompt_version", nullable = false, length = 100)
    private String promptVersion;

    // shortcut: only the input hash is stored; add source revisions when reproducible regeneration is required.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_manifest", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> inputManifest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    private AdvisorReport(Long portfolioId, String modelVersion, String promptVersion,
                          String inputHash, String content, AdviceAction action) {
        this.portfolioId = portfolioId;
        this.modelVersion = modelVersion;
        this.promptVersion = promptVersion;
        this.inputManifest = Map.of("context_sha256", inputHash);
        this.result = Map.of("content", content, "action", action.name());
        this.generatedAt = Instant.now();
    }

    public static AdvisorReport create(Long portfolioId, String modelVersion, String promptVersion,
                                       String inputHash, String content, AdviceAction action) {
        return new AdvisorReport(portfolioId, modelVersion, promptVersion, inputHash, content, action);
    }

    public String getContent() {
        return (String) result.get("content");
    }

    public AdviceAction getAction() {
        return AdviceAction.valueOf((String) result.get("action"));
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.ofInstant(generatedAt, ZoneId.systemDefault());
    }
}
