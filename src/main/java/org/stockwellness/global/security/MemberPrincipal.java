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
        String nickname,
        LoginType loginType,
        MemberRole role,
        Collection<? extends GrantedAuthority> authorities,
        Map<String, Object> attributes
) implements UserDetails, OAuth2User {

    public static MemberPrincipal of(Member member) {
        return new MemberPrincipal(
                member.getId(),
                member.getEmail().getAddress(),
                member.getNickname(),
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
                member.getNickname(),
                member.getLoginType(),
                member.getRole(),
                Collections.singletonList(new SimpleGrantedAuthority(member.getRole().name())),
                attributes
        );
    }

    public static MemberPrincipal fromToken(Long id, String email, String role) {
        return new MemberPrincipal(
                id,
                email,
                null,
                null,
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