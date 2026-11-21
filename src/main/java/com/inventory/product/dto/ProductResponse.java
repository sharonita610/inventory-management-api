package com.inventory.product.dto;

import com.inventory.product.domain.Product;

public record ProductResponse(Long id, String name, long quantity) {

	public static ProductResponse from(final Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getQuantity());
	}
}
