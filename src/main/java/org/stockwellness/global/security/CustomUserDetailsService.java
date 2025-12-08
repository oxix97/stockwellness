package org.stockwellness.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Cacheable(value = "member", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long memeberId = Long.parseLong(username);

        Member member = memberRepository.findById(memeberId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 회원을 찾을 수 없습니다."));

        if (member.isActive())
            throw new DisabledException("비활성화된 계정입니다.");

        return member;
    }
}
