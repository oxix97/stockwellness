package org.stockwellness.adapter.out.persistence.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.application.port.out.member.SaveMemberPort;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.shared.Email;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberAdapter implements LoadMemberPort, SaveMemberPort {

    private final MemberRepository memberRepository;

    @Override
    public Optional<Member> loadMember(Long memberId) {
        return memberRepository.findById(memberId);
    }

    @Override
    public Optional<Member> loadMemberByEmail(Email email) {
        return memberRepository.findByEmail(email);
    }

    @Override
    public Optional<Member> loadMemberByEmailAndLoginType(Email email, LoginType loginType) {
        return memberRepository.findByEmailAndLoginType(email, loginType);
    }

    @Override
    public Member saveMember(Member member) {
        return memberRepository.save(member);
    }
}
