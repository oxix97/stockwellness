package org.stockwellness.global.security.userinfo;

import java.util.Map;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.global.security.userinfo.google.GoogleOAuth2Response;
import org.stockwellness.global.security.userinfo.kakao.KakaoOAuth2Response;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(LoginType loginType, Map<String, Object> attributes) {
        if (loginType == LoginType.KAKAO) {
            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(attributes);
            return OAuth2UserInfo.from(kakaoResponse);
        }

        if (loginType == LoginType.GOOGLE) {
            GoogleOAuth2Response googleResponse = GoogleOAuth2Response.from(attributes);
            return OAuth2UserInfo.from(googleResponse);
        }

        throw new OAuth2AuthenticationException("Unsupported login type: " + loginType);
    }
}
