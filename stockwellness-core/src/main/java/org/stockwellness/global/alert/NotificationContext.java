package org.stockwellness.global.alert;

import java.util.Map;
import lombok.Builder;

@Builder
public record NotificationContext(
    String title,
    String content,
    String traceId,
    String time,
    String userId,
    String url,
    String exceptionType,
    String stackTrace,
    Map<String, String> details, // 배치 통계 등 추가 정보
    NotificationType type
) {
    public enum NotificationType {
        ERROR, SUCCESS, INFO
    }
}
