package org.stockwellness.global.security.userinfo;

import org.stockwellness.domain.member.LoginType;
import org.stockwellness.global.security.userinfo.kakao.KakaoOAuth2Response;

public record OAuth2UserInfo(
        Long id,
        String email,
        String nickname,
        LoginType loginType
) {

    public static OAuth2UserInfo from(KakaoOAuth2Response response) {
        return new OAuth2UserInfo(
                response.id(),
                response.email(),
                response.nickname(),
                LoginType.KAKAO
        );
    }
}
