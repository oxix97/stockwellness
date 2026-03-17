package org.stockwellness.global.logging;

import java.util.Map;

public class TestDto {
    private final String username;
    private final String password;
    @Masked
    private final String email;
    private final String accessToken;
    private final NestedDto nested;

    public TestDto(String username, String password, String email, String accessToken, NestedDto nested) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.accessToken = accessToken;
        this.nested = nested;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String email;
        private String accessToken;
        private NestedDto nested;

        public Builder username(String username) { this.username = username; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder nested(NestedDto nested) { this.nested = nested; return this; }
        public TestDto build() { return new TestDto(username, password, email, accessToken, nested); }
    }

    public static class NestedDto {
        private final String secretValue;
        private final String publicValue;

        public NestedDto(String secretValue, String publicValue) {
            this.secretValue = secretValue;
            this.publicValue = publicValue;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String secretValue;
            private String publicValue;

            public Builder secretValue(String secretValue) { this.secretValue = secretValue; return this; }
            public Builder publicValue(String publicValue) { this.publicValue = publicValue; return this; }
            public NestedDto build() { return new NestedDto(secretValue, publicValue); }
        }
    }
}
