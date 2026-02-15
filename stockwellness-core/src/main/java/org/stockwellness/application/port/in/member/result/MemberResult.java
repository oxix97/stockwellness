package org.stockwellness.application.port.in.member.result;

import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.domain.member.MemberStatus;
import org.stockwellness.domain.member.RiskLevel;

import java.time.LocalDateTime;

public record MemberResult(
        Long id,
        String email,
        String nickname,
        MemberRole role,
        RiskLevel riskLevel,
        MemberStatus status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        PortfolioSummaryResult portfolioSummary
) {
    public static MemberResult from(Member member) {
        return new MemberResult(
                member.getId(),
                member.getEmail().getAddress(),
                member.getNickname(),
                member.getRole(),
                member.getRiskLevel(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getModifiedAt(),
                PortfolioSummaryResult.empty() // 현재는 빈 객체 반환
        );
    }

    // 포트폴리오 확장을 위한 Placeholder DTO
    public record PortfolioSummaryResult(
            Long totalAssetAmount,
            Double totalReturnRate
    ) {
        public static PortfolioSummaryResult empty() {
            return new PortfolioSummaryResult(0L, 0.0);
        }
    }
}