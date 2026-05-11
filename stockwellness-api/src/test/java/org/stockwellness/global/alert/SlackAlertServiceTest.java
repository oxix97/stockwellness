package org.stockwellness.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackAlertServiceTest {

    private final SlackNotificationService slackNotificationService = mock(SlackNotificationService.class);
    private final SlackAlertService service = new SlackAlertService(slackNotificationService);

    @Test
    @DisplayName("에러 발생 시 NotificationContext를 생성하여 전송한다")
    void sendErrorAlert_success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setQueryString("id=1");
        
        setupAuthentication("123");

        Exception e = new RuntimeException("test error");
        String traceId = "abc12345";

        // when
        service.sendErrorAlert(e, traceId, request);

        // then
        verify(slackNotificationService).sendNotification(argThat(context -> 
            context.traceId().equals(traceId) &&
            context.userId().equals("123") &&
            context.url().equals("/api/test?id=1") &&
            context.type() == NotificationContext.NotificationType.ERROR
        ));
        
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("로그인하지 않은 경우 GUEST로 전송한다")
    void sendErrorAlert_guest() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        SecurityContextHolder.clearContext();

        Exception e = new RuntimeException("test error");
        String traceId = "abc12345";

        // when
        service.sendErrorAlert(e, traceId, request);

        // then
        verify(slackNotificationService).sendNotification(argThat(context -> 
            context.userId().equals("GUEST")
        ));
    }

    private void setupAuthentication(String userId) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(userId);
        
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
