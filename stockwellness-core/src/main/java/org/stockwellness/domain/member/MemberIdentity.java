package org.stockwellness.domain.member;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member_identity", uniqueConstraints = @UniqueConstraint(
        name = "uq_member_identity_provider_subject", columnNames = {"provider", "subject"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    private MemberIdentity(Member member, String provider, String subject) {
        this.member = member;
        this.provider = provider;
        this.subject = subject;
    }

    static MemberIdentity of(Member member, String provider, String subject) {
        return new MemberIdentity(member, provider, subject);
    }
}
