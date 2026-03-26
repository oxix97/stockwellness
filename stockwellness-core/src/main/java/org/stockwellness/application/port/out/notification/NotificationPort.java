package org.stockwellness.application.port.out.notification;

/**
 * 외부 시스템(Slack, Email 등)으로 알림을 보내기 위한 Port 인터페이스
 */
public interface NotificationPort {
    void send(String title, String content);
}
