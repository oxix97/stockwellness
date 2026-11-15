package org.stockwellness.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CacheKeyNamespaceTest {
    @Test
    void namespace_is_prepended_to_the_existing_cache_name_prefix() {
        assertThat(CacheKeyNamespace.prefix("new-db-v1", "marketDashboard:v2"))
                .isEqualTo("new-db-v1::marketDashboard:v2::");
    }

    @Test
    void blank_namespace_preserves_the_existing_cache_key_prefix() {
        assertThat(CacheKeyNamespace.prefix(" ", "marketDashboard:v2"))
                .isEqualTo("marketDashboard:v2::");
    }

    @Test
    void invalid_cache_namespaces_are_rejected() {
        assertThatThrownBy(() -> CacheKeyNamespace.prefix("new::db", "cache"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
