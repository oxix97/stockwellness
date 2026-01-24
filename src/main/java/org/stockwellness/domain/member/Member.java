package org.stockwellness.domain.member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.shared.Email;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;
import static org.stockwellness.domain.member.MemberStatus.ACTIVE;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
public class Member extends AbstractEntity {
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
        member.status = ACTIVE;
        return member;
    }

    private static String validateNickname(String nickname) {
        requireNonNull(nickname, "닉네임은 null일 수 없습니다.");
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 공백만으로 이루어질 수 없습니다.");
        }
        if (nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 20자를 초과할 수 없습니다.");
        }
        return nickname;
    }

    @JsonIgnore
    public boolean isActive() {
        return this.status == ACTIVE;
    }

    public void activate() {
        this.status = ACTIVE;
    }

    public void deactivate() {
        this.status = MemberStatus.DEACTIVATED;
    }
}
