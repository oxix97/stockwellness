package org.stockwellness.fixture;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;

public class MemberFixture {

    public static final String EMAIL = "test@example.com";
    public static final String NICKNAME = "tester";
    public static final LoginType LOGIN_TYPE = LoginType.GOOGLE;
    public static final Long MEMBER_ID = 1L;

    public static Member createMember() {
        return Member.register(EMAIL, NICKNAME, LOGIN_TYPE);
    }
}
