package com.inventory.product.dto;

import java.time.Instant;

import com.inventory.product.domain.StockHistory;
import com.inventory.product.domain.StockMovementType;

public record StockHistoryResponse(
	Long id, StockMovementType type, long quantity, long quantityAfter, Instant createdAt
) {

	public static StockHistoryResponse from(final StockHistory history) {
		return new StockHistoryResponse(history.getId(), history.getType(), history.getQuantity(),
			history.getQuantityAfter(), history.getCreatedAt());
	}
}
