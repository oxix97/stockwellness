package org.stockwellness.application.port.in.auth.command;

import org.stockwellness.domain.member.LoginType;

public record LoginCommand(
    String email,
    String nickname,
    LoginType loginType
) {}
