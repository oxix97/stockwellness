package org.stockwellness.application.port.in.auth;

import org.stockwellness.application.port.in.auth.command.LoginCommand;
import org.stockwellness.application.port.in.auth.result.LoginResult;
import org.stockwellness.application.port.in.auth.result.ReissueResult;

public interface AuthUseCase {
    LoginResult login(LoginCommand command);
    ReissueResult reissue(String refreshToken);
    void logout(Long memberId);
}
