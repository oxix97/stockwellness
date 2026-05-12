package org.stockwellness.adapter.in.web.admin;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stockwellness.adapter.in.web.admin.dto.AdminHealthResponse;
import org.stockwellness.global.common.response.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping
    public ApiResponse<AdminHealthResponse> getHealth() {
        HealthComponent healthContribution = healthEndpoint.health();
        
        Map<String, String> statusMap = new HashMap<>();
        
        if (healthContribution instanceof CompositeHealth compositeHealth) {
            Map<String, HealthComponent> components = compositeHealth.getComponents();
            statusMap.put("db", getComponentStatus(components, "db"));
            statusMap.put("redis", getComponentStatus(components, "redis"));
            statusMap.put("kafka", getComponentStatus(components, "kafka"));
        } else {
            // 전체 상태만 있고 하위 컴포넌트가 없는 경우 (fallback)
            String overall = healthContribution.getStatus().getCode();
            statusMap.put("db", overall);
            statusMap.put("redis", overall);
            statusMap.put("kafka", overall);
        }

        return ApiResponse.success(new AdminHealthResponse(
                healthContribution.getStatus().getCode(),
                statusMap
        ));
    }

    private String getComponentStatus(Map<String, HealthComponent> components, String key) {
        HealthComponent component = components.get(key);
        return (component != null) ? component.getStatus().getCode() : "UNKNOWN";
    }
}
