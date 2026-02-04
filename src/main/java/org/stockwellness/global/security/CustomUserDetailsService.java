package org.stockwellness.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.domain.member.Member;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final LoadMemberPort loadMemberPort;

    @Override
    public UserDetails loadUserByUsername(String userIdStr) throws UsernameNotFoundException {
        Long memberId = Long.parseLong(userIdStr);
        Member member = loadMemberPort.loadMember(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found with id: " + memberId));

        return MemberPrincipal.of(member);
    }
}
