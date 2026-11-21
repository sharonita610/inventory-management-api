package com.inventory.product.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.common.validation.ValidationMessages;
import com.inventory.common.web.PageRequests;
import com.inventory.common.web.TextNormalizer;
import com.inventory.product.dto.InboundRequest;
import com.inventory.product.dto.OutboundRequest;
import com.inventory.product.dto.PageResponse;
import com.inventory.product.dto.ProductResponse;
import com.inventory.product.dto.StockHistoryResponse;
import com.inventory.product.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;

@Tag(name = "재고 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InventoryController {

	private final InventoryService inventoryService;

	@Operation(summary = "입고")
	@PostMapping("/inbounds")
	public ProductResponse inbound(@Valid @RequestBody final InboundRequest request) {
		return inventoryService.inbound(request.name(), request.quantity());
	}

	@Operation(summary = "출고")
	@PostMapping("/products/{productId}/outbounds")
	public ProductResponse outbound(
		@PathVariable @Positive(message = ValidationMessages.PRODUCT_ID_POSITIVE) final long productId,
		@Valid @RequestBody final OutboundRequest request
	) {
		return inventoryService.outbound(productId, request.quantity());
	}

	@Operation(summary = "재고 조회")
	@GetMapping("/products/{productId}")
	public ProductResponse getProduct(
		@PathVariable @Positive(message = ValidationMessages.PRODUCT_ID_POSITIVE) final long productId
	) {
		return inventoryService.getProduct(productId);
	}

	@Operation(summary = "상품 목록 조회")
	@GetMapping("/products")
	public PageResponse<ProductResponse> getProducts(
		@RequestParam(required = false) final String name,
		@RequestParam(defaultValue = "0") @PositiveOrZero(message = ValidationMessages.PAGE_RANGE) final int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = ValidationMessages.SIZE_RANGE)
		@Max(value = 100, message = ValidationMessages.SIZE_RANGE) final int size,
		@RequestParam(defaultValue = "id")
		@Pattern(regexp = PageRequests.PRODUCT_SORT_PATTERN, message = ValidationMessages.PRODUCT_SORT_INVALID)
		final String sort,
		@RequestParam(defaultValue = "asc")
		@Pattern(regexp = PageRequests.DIRECTION_PATTERN, message = ValidationMessages.DIRECTION_INVALID)
		final String direction
	) {
		return inventoryService.getProducts(
			TextNormalizer.collapseWhitespace(name), PageRequests.of(page, size, sort, direction));
	}

	@Operation(summary = "입출고 이력 조회")
	@GetMapping("/products/{productId}/histories")
	public PageResponse<StockHistoryResponse> getHistories(
		@PathVariable @Positive(message = ValidationMessages.PRODUCT_ID_POSITIVE) final long productId,
		@RequestParam(defaultValue = "0") @PositiveOrZero(message = ValidationMessages.PAGE_RANGE) final int page,
		@RequestParam(defaultValue = "20") @Min(value = 1, message = ValidationMessages.SIZE_RANGE)
		@Max(value = 100, message = ValidationMessages.SIZE_RANGE) final int size,
		@RequestParam(defaultValue = "id")
		@Pattern(regexp = PageRequests.HISTORY_SORT_PATTERN, message = ValidationMessages.HISTORY_SORT_INVALID)
		final String sort,
		@RequestParam(defaultValue = "desc")
		@Pattern(regexp = PageRequests.DIRECTION_PATTERN, message = ValidationMessages.DIRECTION_INVALID)
		final String direction
	) {
		return inventoryService.getHistories(productId, PageRequests.of(page, size, sort, direction));
	}
}
