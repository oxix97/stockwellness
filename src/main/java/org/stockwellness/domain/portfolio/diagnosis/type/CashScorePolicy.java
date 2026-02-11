package org.stockwellness.domain.portfolio.diagnosis.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashScorePolicy {
    DEFENSE(100),
    ATTACK(40),
    ENDURANCE(100),
    AGILITY(40);

    private final int score;
}
