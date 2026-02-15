package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiagnosisCategory {
    DEFENSE("defense"),
    ATTACK("attack"),
    ENDURANCE("endurance"),
    AGILITY("agility"),
    BALANCE("balance");

    private final String key;
}
