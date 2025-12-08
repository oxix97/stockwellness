package org.stockwellness.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null; // 비로그인 상태 허용 (예: 공개 API)
        }

        if (authentication.getPrincipal() instanceof UserDetails userDetails) {
            return Long.valueOf(userDetails.getUsername()); // memberId
        }
        return null;
    }
}