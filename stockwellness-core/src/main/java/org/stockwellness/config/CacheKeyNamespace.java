package org.stockwellness.config;

public final class CacheKeyNamespace {
    private CacheKeyNamespace() {}

    public static String prefix(String namespace, String cacheName) {
        if (cacheName == null || cacheName.isBlank()) throw new IllegalArgumentException("Cache name is required");
        if (namespace == null || namespace.isBlank()) return cacheName + "::";
        String normalized = namespace.trim();
        if (normalized.contains("::")) throw new IllegalArgumentException("Cache namespace cannot contain the delimiter");
        return normalized + "::" + cacheName + "::";
    }
}
