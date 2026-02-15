package org.stockwellness.fixture;

import org.stockwellness.application.port.in.auth.dto.LoginRequest;
import org.stockwellness.application.port.in.auth.dto.ReissueRequest;
import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.domain.auth.RefreshToken;

import java.time.LocalDateTime;

public class AuthFixture extends MemberFixture {

    public static final String ACCESS_TOKEN = "access.token.dummy";
    public static final String REFRESH_TOKEN = "refresh.token.dummy";

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
