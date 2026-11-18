package org.stockwellness.domain.member;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import static jakarta.persistence.CascadeType.ALL;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.member.exception.MemberDomainException;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.domain.shared.Email;
import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;
import static org.stockwellness.domain.member.MemberStatus.ACTIVE;

@Entity
@Table(name = "member")
@Getter
@ToString
@NoArgsConstructor(access = PROTECTED)
public class Member extends AbstractEntity {
    @Transient
    private Email email;

    @Transient
    private LoginType loginType;

    @Column(nullable = false, length = 20, unique = true)
    private String nickname;

    @Enumerated(STRING)
    @Column(nullable = false)
    private MemberRole role = MemberRole.USER;

    @Enumerated(STRING)
    private RiskLevel riskLevel;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(nullable = false)
    private boolean notificationRebalancing = true;

    @Column(nullable = false)
    private boolean notificationMarketAlert = false;

    @Column(nullable = false)
    private boolean notificationNewListing = false;

    @OneToMany(mappedBy = "member", cascade = ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<MemberIdentity> identities = new ArrayList<>();

    public static Member register(String email, String nickname, LoginType loginType) {
        Member member = new Member();
        member.email = new Email(email);
        member.nickname = validateNickname(nickname);
        member.riskLevel = RiskLevel.MEDIUM;
        member.loginType = loginType;
        member.status = ACTIVE;
        member.identities.add(MemberIdentity.of(member, loginType.name(), email));
        return member;
    }

    @PostLoad
    private void loadLoginIdentity() {
        if (identities == null || identities.isEmpty()) return;
        MemberIdentity identity = identities.getFirst();
        this.email = new Email(identity.getSubject());
        this.loginType = LoginType.valueOf(identity.getProvider());
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

    public void update(String nickname, RiskLevel riskLevel) {
        if (nickname != null) {
            this.nickname = validateNickname(nickname);
        }
        if (riskLevel != null) {
            this.riskLevel = riskLevel;
        }
    }

    public void updateNotifications(Boolean rebalancing, Boolean marketAlert, Boolean newListing) {
        if (rebalancing != null) this.notificationRebalancing = rebalancing;
        if (marketAlert != null) this.notificationMarketAlert = marketAlert;
        if (newListing != null) this.notificationNewListing = newListing;
    }

    private static String validateNickname(String nickname) {
        if (nickname == null) {
            throw new MemberDomainException("닉네임은 null일 수 없습니다.");
        }
        if (nickname.isBlank()) {
            throw new MemberDomainException("닉네임은 공백만으로 이루어질 수 없습니다.");
        }
        if (nickname.length() > 20) {
            throw new MemberDomainException("닉네임은 20자를 초과할 수 없습니다.");
        }
        return nickname;
    }
}
