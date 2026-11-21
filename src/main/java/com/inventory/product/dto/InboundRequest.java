package com.inventory.product.dto;

import com.inventory.common.validation.ValidationMessages;
import com.inventory.common.web.TextNormalizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InboundRequest(
	@NotBlank(message = ValidationMessages.PRODUCT_NAME_REQUIRED)
	@Size(max = 100, message = ValidationMessages.PRODUCT_NAME_TOO_LONG)
	String name,

	@NotNull(message = ValidationMessages.QUANTITY_REQUIRED)
	@Positive(message = ValidationMessages.QUANTITY_POSITIVE)
	Long quantity) {

	public InboundRequest {
		name = TextNormalizer.collapseWhitespace(name);
	}
}
