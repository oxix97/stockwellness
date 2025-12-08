package org.stockwellness.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.shared.Email;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static jakarta.persistence.EnumType.STRING;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;
import static org.stockwellness.domain.member.MemberStatus.ACTIVE;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
public class Member extends AbstractEntity implements UserDetails {
    @Embedded
    private Email email;

    @Column(nullable = false, length = 20, unique = true)
    private String nickname;

    @Enumerated(STRING)
    private LoginType loginType;

    @Enumerated(STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.USER;

    @Enumerated(STRING)
    private RiskLevel riskLevel;

    @Enumerated(STRING)
    private MemberStatus status;

    @LastModifiedDate
    private LocalDateTime modifiedAt;

    public static Member register(String email, String nickname,LoginType loginType) {
        Member member = new Member();
        member.email = new Email(email);
        member.nickname = validateNickname(nickname);
        member.riskLevel = RiskLevel.MEDIUM;
        member.loginType = loginType;
        member.status = MemberStatus.PENDING;
        return member;
    }

    private static String validateNickname(String nickname) {
        requireNonNull(nickname, "닉네임은 null일 수 없습니다.");
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 공백만으로 이루어질 수 없습니다.");
        }
        return nickname;
    }

    public boolean isActive() {
        return this.status == ACTIVE;
    }

    public void activate() {
        this.status = ACTIVE;
    }

    public void deactivate() {
        this.status = MemberStatus.DEACTIVATED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.getId().toString();
    }
}
