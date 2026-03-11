package org.stockwellness.adapter.in.web.admin.dto;

import java.util.Map;

public record AdminHealthResponse(
    String status,
    Map<String, String> components
) {}
