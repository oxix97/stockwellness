package org.stockwellness.application.port.in.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(@NotBlank String refreshToken) {

}