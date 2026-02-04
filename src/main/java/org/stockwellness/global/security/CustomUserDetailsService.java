package org.stockwellness.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.exception.MemberNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final LoadMemberPort loadMemberPort;

    @Override
    @Cacheable(value = "member", key = "#memberIdStr")
    public UserDetails loadUserByUsername(String memberIdStr) {
        Long memberId = Long.parseLong(memberIdStr);
        Member member = loadMemberPort.loadMember(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return MemberPrincipal.of(member);
    }
}
