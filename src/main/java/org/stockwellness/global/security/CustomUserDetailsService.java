package org.stockwellness.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.stockwellness.application.port.out.member.LoadMemberPort;
import org.stockwellness.domain.member.Member;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final LoadMemberPort loadMemberPort;

    @Override
    @Cacheable(value = "member", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long memberId = Long.parseLong(username);

        Member member = loadMemberPort.loadMember(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));

        if (!member.isActive())
            throw new DisabledException("비활성화된 계정입니다.");

        return new CustomUserDetails(member);
    }
}
