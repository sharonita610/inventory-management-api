package com.inventory.product.dto;

import com.inventory.common.validation.ValidationMessages;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OutboundRequest(
	@NotNull(message = ValidationMessages.QUANTITY_REQUIRED)
	@Positive(message = ValidationMessages.QUANTITY_POSITIVE)
	Long quantity) {
}
