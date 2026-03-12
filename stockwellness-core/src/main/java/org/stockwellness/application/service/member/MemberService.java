package org.stockwellness.application.service.member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.member.MemberUseCase;
import org.stockwellness.application.port.in.member.command.UpdateMemberCommand;
import org.stockwellness.application.port.in.member.result.MemberResult;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.exception.MemberNotFoundException;
import org.stockwellness.domain.member.exception.NicknameDuplicateException;
import org.stockwellness.global.error.exception.GlobalException;

import static org.stockwellness.global.error.ErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MemberService implements MemberUseCase {

    private final LoadMemberPort loadMemberPort;

    @Override
    public MemberResult getMember(Long memberId) {
        var member = findMember(memberId);
        return MemberResult.from(member);
    }

    @Override
    public void updateMember(Long memberId, UpdateMemberCommand command) {
        var member = findMember(memberId);

        if (!member.isActive()) {
            throw new GlobalException(UNAUTHORIZED);
        }

        if (command.nickname() != null && !member.getNickname().equals(command.nickname())) {
            if (loadMemberPort.existsByNickname(command.nickname())) {
                throw new NicknameDuplicateException();
            }
        }
        member.update(command.nickname(), command.riskLevel());
    }

    @Override
    public void withdrawMember(Long memberId) {
        var member = findMember(memberId);
        member.deactivate();
    }

    private Member findMember(Long memberId) {
        return loadMemberPort.loadMember(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }
}
