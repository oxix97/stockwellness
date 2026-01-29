package org.stockwellness.application.port.in.member.command;

import org.stockwellness.domain.member.RiskLevel;

public record UpdateMemberCommand(
        Long memberId,
        String nickname,
        RiskLevel riskLevel
) {
    public static UpdateMemberCommand of(Long memberId, String nickname, RiskLevel riskLevel) {
        return new UpdateMemberCommand(memberId, nickname, riskLevel);
    }
}
