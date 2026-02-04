package org.stockwellness.global.security.userinfo.google;

import java.util.Map;

public record GoogleOAuth2Response(
        String sub,
        String name,
        String givenName,
        String familyName,
        String picture,
        String email,
        boolean emailVerified
) {
    public static GoogleOAuth2Response from(Map<String, Object> attributes) {
        return new GoogleOAuth2Response(
                String.valueOf(attributes.get("sub")),
                String.valueOf(attributes.get("name")),
                String.valueOf(attributes.get("given_name")),
                String.valueOf(attributes.get("family_name")),
                String.valueOf(attributes.get("picture")),
                String.valueOf(attributes.get("email")),
                Boolean.parseBoolean(String.valueOf(attributes.get("email_verified")))
        );
    }
}
