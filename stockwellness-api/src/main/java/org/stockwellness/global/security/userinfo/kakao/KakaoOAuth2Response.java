package org.stockwellness.global.security.userinfo.kakao;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unchecked")
public record KakaoOAuth2Response(
        Long id,
        Map<String, Object> properties,
        KakaoAccount kakaoAccount
) {
    public static KakaoOAuth2Response from(Map<String, Object> attributes) {
        return new KakaoOAuth2Response(
                Optional.ofNullable(attributes.get("id"))
                        .map(obj -> Long.valueOf(obj.toString()))
                        .orElseThrow(() -> new IllegalArgumentException("카카오 아이디가 존재하지 않습니다.")),
                (Map<String, Object>) attributes.getOrDefault("properties", Map.of()),
                KakaoAccount.from((Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of()))
        );
    }

    public String email() {
        return this.kakaoAccount().email();
    }

    public String nickname() {
        return this.kakaoAccount().nickname();
    }
}