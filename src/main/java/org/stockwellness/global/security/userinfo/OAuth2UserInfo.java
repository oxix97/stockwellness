package org.stockwellness.global.security.userinfo;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.global.security.userinfo.google.GoogleOAuth2Response;
import org.stockwellness.global.security.userinfo.kakao.KakaoOAuth2Response;

public record OAuth2UserInfo(
        String providerId,
        String email,
        String nickname,
        LoginType loginType
) {

    public static OAuth2UserInfo from(KakaoOAuth2Response response) {
        return new OAuth2UserInfo(
                String.valueOf(response.id()),
                response.email(),
                response.nickname(),
                LoginType.KAKAO
        );
    }

    public static OAuth2UserInfo from(GoogleOAuth2Response response) {
        return new OAuth2UserInfo(
                response.sub(),
                response.email(),
                response.name(),
                LoginType.GOOGLE
        );
    }
}
