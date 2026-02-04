package org.stockwellness.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;
import org.stockwellness.domain.member.MemberRole;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record MemberPrincipal(
        Long id,
        String email,
        LoginType loginType,
        MemberRole role,
        Collection<? extends GrantedAuthority> authorities,
        Map<String, Object> attributes
) implements UserDetails, OAuth2User {

    public static MemberPrincipal of(Member member) {
        return new MemberPrincipal(
                member.getId(),
                member.getEmail().getAddress(),
                member.getLoginType(),
                member.getRole(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name())),
                null
        );
    }

    public static MemberPrincipal of(Member member, Map<String, Object> attributes) {
        return new MemberPrincipal(
                member.getId(),
                member.getEmail().getAddress(),
                member.getLoginType(),
                member.getRole(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name())),
                attributes
        );
    }

    public static MemberPrincipal fromToken(Long id, String email, String role) {
        // 토큰에서 복원 시 LoginType이나 MemberRole의 정확한 Enum 매핑이 필요하다면 추가 로직 필요.
        // 여기서는 Role만 String으로 받아서 처리하거나, null로 처리.
        // 현재 로직상 토큰 검증 필터에서는 Principal을 만들 때 LoginType이 필수적이지 않을 수 있음.
        return new MemberPrincipal(
                id,
                email,
                null, // LoginType from token? (claim에 있다면 복원 가능)
                MemberRole.valueOf(role),
                Collections.singletonList(new SimpleGrantedAuthority(role)),
                null
        );
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public String getName() {
        return id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}