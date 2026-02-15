package org.stockwellness.adapter.in.web.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.domain.member.RiskLevel;

public record UpdateMemberRequest(
        @Size(min = 1, max = 20, message = "닉네임은 20자 이하여야 합니다.")
        @Pattern(regexp = "^\\S+$", message = "닉네임에 공백을 포함할 수 없습니다.")
        String nickname,
        RiskLevel riskLevel
) {
    public UpdateMemberCommand toCommand(Long memberId) {
        return UpdateMemberCommand.of(memberId, nickname, riskLevel);
    }
}
