package org.stockwellness.support.annotation;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.MemberRole;
import org.stockwellness.global.security.MemberPrincipal;

import java.util.Collections;
import java.util.List;

public class MockMemberSecurityContextFactory implements WithSecurityContextFactory<MockMember> {

    @Override
    public SecurityContext createSecurityContext(MockMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        MemberPrincipal principal = new MemberPrincipal(
                annotation.id(),
                annotation.email(),
                annotation.nickname(),
                LoginType.KAKAO, // 기본값 설정
                MemberRole.valueOf(annotation.role()),
                Collections.singletonList(new SimpleGrantedAuthority(annotation.role())),
                null
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                "",
                principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
