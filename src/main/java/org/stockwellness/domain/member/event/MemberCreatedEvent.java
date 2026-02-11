package org.stockwellness.domain.member.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.stockwellness.domain.member.Member;

@Getter
@RequiredArgsConstructor
public class MemberCreatedEvent {
    private final Member member;
}
