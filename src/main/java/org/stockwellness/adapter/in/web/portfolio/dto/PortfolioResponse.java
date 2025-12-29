package org.stockwellness.adapter.in.web.portfolio.dto;

import org.stockwellness.domain.portfolio.Portfolio;

import java.util.List;

public record PortfolioResponse(
        Long id,
        String name,
        String description,
        int totalPieces,
        List<PortfolioItemResponse> items
) {
    public static PortfolioResponse from(Portfolio entity) {
        return new PortfolioResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTotalPieces(),
                entity.getItems().stream().map(PortfolioItemResponse::from).toList()
        );
    }
}