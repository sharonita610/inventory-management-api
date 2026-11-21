package com.inventory.product.domain;

import java.time.Instant;

import com.inventory.common.error.BusinessException;
import com.inventory.common.error.ErrorCode;
import com.inventory.common.validation.ValidationMessages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100, updatable = false)
	private String name;

	@Column(nullable = false)
	private long quantity;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false, insertable = false)
	private Instant updatedAt;

	public static Product of(final String name) {
		return new Product(name);
	}

	private Product(final String name) {
		this.name = name;
		this.quantity = 0;
	}

	public StockHistory inbound(final long amount) {
		requirePositive(amount);
		if (amount > Long.MAX_VALUE - this.quantity) {
			throw new BusinessException(ErrorCode.STOCK_LIMIT_EXCEEDED);
		}
		this.quantity += amount;
		return StockHistory.of(this, StockMovementType.INBOUND, amount);
	}

	public StockHistory outbound(final long amount) {
		requirePositive(amount);
		if (this.quantity < amount) {
			throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
				"재고가 부족합니다. 현재 재고: %d, 요청 수량: %d".formatted(this.quantity, amount));
		}
		this.quantity -= amount;
		return StockHistory.of(this, StockMovementType.OUTBOUND, amount);
	}

	private void requirePositive(final long amount) {
		if (amount <= 0) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, ValidationMessages.QUANTITY_POSITIVE);
		}
	}

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}
}
