package org.stockwellness.fixture;

import org.stockwellness.adapter.in.web.auth.dto.LoginRequest;
import org.stockwellness.adapter.in.web.auth.dto.ReissueRequest;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.domain.auth.RefreshToken;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.domain.member.Member;

import java.time.LocalDateTime;

public class AuthFixture {

    public static final String EMAIL = "test@example.com";
    public static final String NICKNAME = "tester";
    public static final LoginType LOGIN_TYPE = LoginType.GOOGLE;
    public static final String ACCESS_TOKEN = "access.token.dummy";
    public static final String REFRESH_TOKEN = "refresh.token.dummy";
    public static final Long MEMBER_ID = 1L;

    public static Member createMember() {
        return Member.register(EMAIL, NICKNAME, LOGIN_TYPE);
    }

    public static LoginCommand createLoginCommand() {
        return new LoginCommand(EMAIL, NICKNAME, LOGIN_TYPE);
    }

    public static LoginRequest createLoginRequest() {
        return new LoginRequest(EMAIL, NICKNAME, LOGIN_TYPE);
    }

    public static ReissueRequest createReissueRequest() {
        return new ReissueRequest(REFRESH_TOKEN);
    }

    public static RefreshToken createRefreshToken() {
        return RefreshToken.create(MEMBER_ID, REFRESH_TOKEN, LocalDateTime.now().plusDays(7));
    }
}
