package org.stockwellness.batch.common;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 부모 스레드의 MDC 컨텍스트를 자식 스레드로 전달하는 데코레이터
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 현재 스레드(부모)의 MDC 맵 복사
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // 스레드 풀 재사용 시 오염 방지를 위해 클리어
                MDC.clear();
            }
        };
    }
}
