package com.inventory.product.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stock_history")
public class StockHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private StockMovementType type;

	@Column(nullable = false, updatable = false)
	private long quantity;

	@Column(name = "quantity_after", nullable = false, updatable = false)
	private long quantityAfter;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private StockHistory(
		final Long productId, final StockMovementType type, final long quantity, final long quantityAfter) {
		this.productId = productId;
		this.type = type;
		this.quantity = quantity;
		this.quantityAfter = quantityAfter;
		this.createdAt = Instant.now();
	}

	static StockHistory of(final Product product, final StockMovementType type, final long quantity) {
		return new StockHistory(product.getId(), type, quantity, product.getQuantity());
	}
}
