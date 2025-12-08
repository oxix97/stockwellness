package org.stockwellness.adapter.in.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(@NotBlank String refreshToken) {

}