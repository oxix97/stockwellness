package org.stockwellness.application.port.in.portfolio.result;

public record StockStatResult(
        String isinCode,
        String name,
        int defense,
        int attack,
        int endurance,
        int agility
) {

    public static StockStatResult of(
            String isinCode,
            String name,
            int defense,
            int attack,
            int endurance,
            int agility
    ) {
        return new StockStatResult(isinCode, name, defense, attack, endurance, agility);
    }
}