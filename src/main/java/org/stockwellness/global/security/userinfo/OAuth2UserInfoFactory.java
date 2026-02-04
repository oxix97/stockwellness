package org.stockwellness.global.security.userinfo;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.stockwellness.domain.member.LoginType;
import org.stockwellness.global.security.userinfo.kakao.KakaoOAuth2Response;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(LoginType loginType, Map<String, Object> attributes) {
        if (loginType == LoginType.KAKAO) {
            KakaoOAuth2Response kakaoResponse = KakaoOAuth2Response.from(attributes);
            return OAuth2UserInfo.from(kakaoResponse);
        }

        if (loginType == LoginType.GOOGLE) {
            // MEMO : 나중에 구현.
        }

        throw new OAuth2AuthenticationException("Unsupported login type: " + loginType);
    }
}
