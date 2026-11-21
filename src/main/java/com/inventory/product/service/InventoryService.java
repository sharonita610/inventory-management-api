package com.inventory.product.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.common.error.BusinessException;
import com.inventory.common.error.ErrorCode;
import com.inventory.product.domain.Product;
import com.inventory.product.dto.PageResponse;
import com.inventory.product.dto.ProductResponse;
import com.inventory.product.dto.StockHistoryResponse;
import com.inventory.product.repository.ProductRepository;
import com.inventory.product.repository.StockHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final ProductRepository productRepository;
	private final StockHistoryRepository stockHistoryRepository;

	@Transactional
	public ProductResponse inbound(final String name, final long quantity) {
		productRepository.lockName(name);
		Product product = productRepository.findByNameForUpdate(name)
			.orElseGet(() -> productRepository.save(Product.of(name)));
		stockHistoryRepository.save(product.inbound(quantity));
		return ProductResponse.from(product);
	}

	@Transactional
	public ProductResponse outbound(final long productId, final long quantity) {
		Product product = productRepository.findByIdForUpdate(productId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
		stockHistoryRepository.save(product.outbound(quantity));
		return ProductResponse.from(product);
	}

	@Transactional(readOnly = true)
	public ProductResponse getProduct(final long productId) {
		return productRepository.findById(productId)
			.map(ProductResponse::from)
			.orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> getProducts(final String name, final Pageable pageable) {
		Page<Product> products = Optional.ofNullable(name)
			.filter(value -> !value.isBlank())
			.map(value -> productRepository.findByName(value, pageable))
			.orElseGet(() -> productRepository.findAll(pageable));
		return PageResponse.of(products, ProductResponse::from);
	}

	@Transactional(readOnly = true)
	public PageResponse<StockHistoryResponse> getHistories(final long productId, final Pageable pageable) {
		if (!productRepository.existsById(productId)) {
			throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		return PageResponse.of(
			stockHistoryRepository.findByProductId(productId, pageable),
			StockHistoryResponse::from
		);
	}
}
